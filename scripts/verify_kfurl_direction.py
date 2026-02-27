#!/usr/bin/env python3
"""
KFURL Direction Verification Script

Replicates the EXACT Rlsol.rlsol() formula from Rlsol.kt and traces real
ME7Logger data to determine if KFURL should go UP or DOWN.

This is independent of the Java/Kotlin app — purely Python math.

Usage: python3 scripts/verify_kfurl_direction.py ~/Desktop/input
"""
import os, sys, random
from math import sqrt


def rlsol(pu, ps, tans, tmot, kfurl, plsol, kfprg=70.0, fpbrkds=1.016):
    """Exact replica of Rlsol.rlsol() from Rlsol.kt"""
    KFFWTBR = 0.02
    VPSSPLS = 1.016
    fho = pu / 1013.0
    pirg = fho * kfprg
    pbr = ps * fpbrkds
    psagr = 250.0
    evtmod = tans + (tmot - tans) * KFFWTBR
    fwft = (tans + 673.425) / 731.334
    ftbr = 273.0 / (evtmod + 273) * fwft
    fupsrl = kfurl * ftbr
    rfagr = max(pbr - pirg, 0.0) * fupsrl * psagr / ps
    return (plsol * fupsrl * fpbrkds * VPSSPLS) - rfagr


def grid_search(samples, kfprg=70.0, fpbrkds=1.016):
    """Grid search for optimal KFURL, returns (best_kfurl, best_rmse, rmse_at_106)."""
    best, best_rmse, rmse_106 = 0.106, float('inf'), 0
    for step in range(301):
        k = 0.050 + (0.200 - 0.050) * step / 300
        sq = sum(
            # Use pvdks as ps for rfagr — at WOT, pvdks ≈ manifold pressure >> baro.
            # Using pus (baro) as ps makes rfagr ~2x too large at boost.
            (rlsol(s['pus'], s['pvdks'], 20.0, 96.0, k, s['pvdks'], kfprg, fpbrkds) - s['rl']) ** 2
            for s in samples
        )
        rmse = sqrt(sq / len(samples))
        if abs(k - 0.106) < 0.0005: rmse_106 = rmse
        if rmse < best_rmse: best_rmse = rmse; best = k
    return best, best_rmse, rmse_106


def parse_log(path):
    """Parse a single ME7Logger CSV."""
    with open(path, encoding='latin-1') as f:
        lines = f.readlines()
    hi = None
    for i, l in enumerate(lines):
        if 'TimeStamp' in l and 'nmot' in l:
            hi = i; break
    if hi is None: return None
    h = [c.strip() for c in lines[hi].split(',')]
    def idx(n): return h.index(n) if n in h else None
    col = {k: idx(n) for k, n in [
        ('rpm','nmot'),('mshfm','mshfm_w'),('msdk','msdk_w'),
        ('rl','rl_w'),('pvdks','pvdks_w'),('pus','pus_w'),
        ('wdkba','wdkba'),('tans','tans'),('tmot','tmot')]}
    if col['rpm'] is None: return None
    rows = []
    for line in lines[hi+3:]:
        cs = line.strip().split(',')
        r = {}
        for k, ci in col.items():
            if ci is not None and ci < len(cs):
                try: r[k] = float(cs[ci].strip())
                except: r[k] = None
            else: r[k] = None
        if r.get('rpm'): rows.append(r)
    return rows


def main():
    logdir = sys.argv[1] if len(sys.argv) > 1 else os.path.expanduser('~/Desktop/input')
    if not os.path.isdir(logdir):
        print(f"ERROR: {logdir} not found"); sys.exit(1)
    files = sorted(f for f in os.listdir(logdir) if f.endswith('.csv'))
    print(f"Found {len(files)} logs in {logdir}")

    random.seed(42)
    sel = random.sample(files, min(8, len(files)))

    all_wot, all_samp = [], []
    for fname in sel:
        rows = parse_log(os.path.join(logdir, fname))
        if not rows: continue
        has = lambda k: any(r.get(k) is not None and r[k] > 0.5 for r in rows)
        if not (has('rl') and has('pvdks') and has('pus')): continue
        rl_vals = [r['rl'] for r in rows if r.get('rl') and r['rl'] > 0]
        sc = 100.0 if max(rl_vals) < 3.0 else 1.0
        for r in rows:
            if (r.get('rl') and r.get('pvdks') and r.get('pus')
                and r['rpm'] > 500 and r.get('mshfm',0) > 1.0
                and r['pvdks'] > 100 and r['pus'] > 800 and r['rl'] > 0.5):
                s = {'rpm':r['rpm'],'rl':r['rl']*sc,'pvdks':r['pvdks'],'pus':r['pus'],
                     'wdkba':r.get('wdkba',0)}
                all_samp.append(s)
                # WOT filter: at WOT (wdkba >= 80°), pvdks ≈ manifold pressure (ps)
                # pvdks_w is pre-throttle pressure, NOT manifold pressure
                # The Rlsol formula expects manifold pressure, so only WOT data is valid
                if (r.get('wdkba') or 0) >= 80.0:
                    all_wot.append(s)

    print(f"\nAGGREGATE: {len(all_wot)} WOT (wdkba>=80°), {len(all_samp)} total\n")

    if len(all_wot) >= 20:
        bk, br, r106 = grid_search(all_wot)
        print(f"WOT ONLY:  optimal={bk:.4f}, RMSE@0.106={r106:.2f}%, RMSE@best={br:.2f}%")
        print(f"  Direction: {'UP' if bk > 0.108 else 'DOWN' if bk < 0.104 else 'SAME'}")
    if len(all_samp) >= 20:
        bk, br, r106 = grid_search(all_samp)
        print(f"ALL SAMP:  optimal={bk:.4f}, RMSE@0.106={r106:.2f}%, RMSE@best={br:.2f}%")
        print(f"  Direction: {'UP' if bk > 0.108 else 'DOWN' if bk < 0.104 else 'SAME'}")

    # Spot-check 5 highest-pvdks WOT samples
    top_source = all_wot if all_wot else all_samp
    if top_source:
        top = sorted(top_source, key=lambda s: -s['pvdks'])[:5]
        print(f"\nTop 5 highest-pvdks {'WOT' if all_wot else 'all'} samples:")
        print(f"{'RPM':>6} {'rl_w%':>7} {'pvdks':>7} {'pus':>6} {'wdkba':>6} | {'Rlsol(.08)':>11} {'Rlsol(.106)':>12} {'Rlsol(.12)':>11} | best_k")
        for s in top:
            p08 = rlsol(s['pus'], s['pus'], 20.0, 96.0, 0.080, s['pvdks'])
            p106 = rlsol(s['pus'], s['pus'], 20.0, 96.0, 0.106, s['pvdks'])
            p12 = rlsol(s['pus'], s['pus'], 20.0, 96.0, 0.120, s['pvdks'])
            # Binary search for best kfurl
            lo, hi = 0.050, 0.200
            for _ in range(50):
                mid = (lo+hi)/2
                pred = rlsol(s['pus'], s['pus'], 20.0, 96.0, mid, s['pvdks'])
                if pred < s['rl']: lo = mid
                else: hi = mid
            bk = (lo+hi)/2
            print(f"{s['rpm']:6.0f} {s['rl']:7.1f} {s['pvdks']:7.0f} {s['pus']:6.0f} {s.get('wdkba',0):6.1f} | "
                  f"{p08:11.1f} {p106:12.1f} {p12:11.1f} | {bk:.4f}")


if __name__ == '__main__':
    main()

