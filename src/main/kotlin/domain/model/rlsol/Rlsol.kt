package domain.model.rlsol

import domain.math.Point

class Rlsol(pu: Double, tans: Double, kfurl: Double, pressure: List<Double>) {
    val points: Array<Point>

    init {
        points = Array(pressure.size) { Point(0.0, 0.0) }
        var ps = pressure[0]
        for (i in points.indices) {
            val x = pressure[i]
            val y = rlsol(pu, ps, tans, 96.0, kfurl, x)
            ps = x
            points[i] = Point(x, y)
        }
    }

    companion object {
        /**
         * Compute rl (relative load) from manifold pressure.
         *
         * BGSRM VE model reverse path (me7-raw.txt line 54297–54327):
         *   rl = fupsrl_w × (ps - pirg_w) × FPBRKDS × VPSSPLS - rfagr
         *
         * @param pu Barometric pressure (mbar)
         * @param ps Previous manifold pressure estimate (mbar)
         * @param tans Intake air temperature (°C)
         * @param tmot Coolant temperature (°C)
         * @param kfurl VE slope constant (%/hPa)
         * @param plsol Actual manifold pressure (mbar)
         * @param kfprg Residual gas partial pressure (hPa) — from KFPRG map or default 70.0
         * @param fpbrkds Combustion chamber pressure correction factor — from KFPBRK map or default 1.016
         */
        fun rlsol(
            pu: Double, ps: Double, tans: Double, tmot: Double, kfurl: Double, plsol: Double,
            kfprg: Double = 70.0,
            fpbrkds: Double = 1.016
        ): Double {
            val KFFWTBR = 0.02
            val VPSSPLS = 1.016

            val fho = pu / 1013.0
            val pirg = fho * kfprg
            val pbr = ps * fpbrkds
            val psagr = 250.0

            val evtmod = tans + (tmot - tans) * KFFWTBR
            val fwft = (tans + 673.425) / 731.334
            val ftbr = 273.0 / (evtmod + 273) * fwft

            val fupsrl = kfurl * ftbr
            val rfagr = maxOf(pbr - pirg, 0.0) * fupsrl * psagr / ps

            return (plsol * fupsrl * fpbrkds * VPSSPLS) - rfagr
        }
    }
}
