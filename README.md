# ME7Tuner

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

ME7Tuner is software that provides tools to help calibrate the MAF, primary fueling and torque/load requests. It is
primarily designed for the ME7 M-box ECU (Audi B5 S4 2.7T biturbo), but the XDF parser supports the full TunerPro XDF
format so many ME7 / ME7.1 variant ECUs can be used with the correct XDF and configuration file.

<img src="/documentation/images/me7Tuner.png" width="800">

# Warning

ME7Tuner is free software written by some guy on the internet. ***ME7Tuner comes with no warranty.*** Use at your own risk!

It is a certainty that ME7Tuner will produce garbage outputs at some point you will damage your engine if you do not know what you are doing.
ME7Tuner is software that *helps* you calibrate your engine. It does not calibrate your engine for you. It is not a replacement for knowledge of how to calibrate an engine.

## Installation

ME7Tuner is packaged as a JAR file. You will need to have Java installed on your system to run it. You can download Java from [here](https://www.oracle.com/java/technologies/downloads/).

Note that ME7Tuner built against Java 17, so you will need to have Java 17+ installed on your system to run it. Once you have Java 17+ installed and the JAR file, simply double click the JAR file to run it.

<a href="https://github.com/KalebKE/ME7Tuner/releases/latest">![GitHub Release](https://img.shields.io/github/v/release/KalebKE/ME7Tuner?color=GREEN)</a>

# Do I Need ME7Tuner?

You probably don't need to use ME7Tuner. For most applications, the stock M-box is sufficient to support the power you want to make.

In general ME7Tuner is only useful if you need to request more than 191% load on an M-Box. This means that K03 and most K04 configurations do not need the level of calibrations provided by ME7Tuner.

The following information should give you a good estimate of what hardware you need to achieve a given power goal, how much calibration you will need to do to support that power and if ME7Tuner is useful to you.

##### Stock MAP Limit

The stock MAP limit is the primary limitation to calibration. A stock M-box has just enough overhead to support K04's within their optimal efficiency range.

* 2.5bar absolute (~22.45 psi relative)

Read [MAP Sensor](https://s4wiki.com/wiki/Manifold_air_pressure)
Read [5120 Hack](http://nefariousmotorsports.com/forum/index.php?topic=3027.0)

Unless you are planning on making more than 2.5 bar absolute (~22.45 psi relative) of pressure, you don't need to use ME7Tuner.

##### Turbo Airflow

* K03 16 lbs/min (120 g/sec) (~160hp)
* K04 22 lbs/min (166 g/sec) (~225hp)
* RS6 25 lbs/min  (196 g/sec) (~265hp)
* 650R 37 lbs/min (287 g/sec) (~370hp)
* 770R 48 lbs/min (370 g/sec) ((~490hp)

Note: Remember to multiply by the number of turbo. The 2.7T has two turbos.

Unless you are planning on maxing your K04's, or have a larger turbo frame, you don't need to use ME7Tuner.

##### MAF Airflow

* Stock Bosch/Hitachi 73mm (337 g/sec)
* Stock RS4 83mm (498 g/sec)
* Stock Hitachi 85mm (493 g/sec)
* HPX 89mm (800+ g/sec)

Read [MAF Sensor](https://s4wiki.com/wiki/Mass_air_flow)

Unless you are planning on maxing the stock MAF, or have a larger MAF, you don't need to use ME7Tuner.

##### Fuel for Airflow (10:1 AFR)

* K03 16 lbs/min air ->  ~1000 cc/min fuel
* K04 22 lbs/min air -> ~1400 cc/min fuel
* RS6 25 lbs/min air -> ~1600 cc/min fuel
* 650R 37 lbs/min air -> ~2200 cc/min fuel
* 770R 48 lbs/min air -> ~3024 cc/min fuel

Note: Remember to multiply air by the number of turbos and divide fuel by the number of fuel injectors. The 2.7T has 6 fuel injectors.

##### Theoretical fuel injector size for a V6 bi-turbo configuration

* K03 16 lbs/min air -> ~340 cc/min
* K04 22 lbs/min air -> ~470 cc/min
* RS6 25 lbs/min air -> ~540 cc/min
* 650R 37 lbs/min air -> ~740 cc/min
* 770R 48 lbs/min air -> ~1000 cc/min

Read [Fuel Injectors](https://s4wiki.com/wiki/Fuel_injectors)

##### Theoretical load for a 2.7l V6 configuration

* K03 16 lbs/min air -> ~155% load -> ~320hp
* K04 22 lbs/min air -> ~210% load -> ~440hp
* RS6 25 lbs/min air -> ~240% load -> ~500hp
* 650R 37 lbs/min air -> ~354% load -> ~740hp
* 770R 48 lbs/min air -> ~460% load -> ~960hp

Note that a stock M-box has a maximum load request of 191%, but can be increased with the stock MAP sensor to ~215%.

# How ME7 Works

Everything in ME7 revolves around requested load (or cylinder fill).

* Read [Engine load](https://s4wiki.com/wiki/Load)

In ME7, the driver uses the accelerator pedal position to make a torque request. Pedal positions are mapped to a torque request (which is effectively a normalized load request). That torque request is then mapped to a load request. ME7 calculates how much pressure (boost) is required to achieve the load request which is highly dependent on hardware (the engine, turbo, etc...) and also the weather (cold, dry air is denser than hot, moist air). When tuning ME7 the goal is to calibrate the various maps to model the hardware and the weather accurately. If modeled incorrectly, ME7 will determine something is wrong and will protect the engine by reducing the capacity to produce power at various levels of intervention.

Note that no amount of modifications (intake, exhaust, turbo, boost controllers, etc...) will increase the power of the engine if actual load is already equal to or greater than requested load. ME7 will use interventions to *decrease* actual load (power) to get it equal requested load. You must calibrate the tune to request more load to increase power.

ME7Tuner can provide calculations that allow ME7 to be tuned with accurate airflow, pressure and load measurements which can simplify calibrations.

# Workflow Overview

ME7Tuner is organized into three stages that mirror the calibration workflow:

| Stage | When You Need It | What It Does |
|-------|-----------------|--------------|
| **Configuration** | Everyone | Load BIN + XDF, select map definitions, configure log headers |
| **Calibration** | Modified engines only | Recalibrate fueling, MAF, torque/load tables, ignition, and throttle maps for new hardware |
| **Optimization** | Everyone | Analyze WOT logs and correct boost control + VE model to match requested targets |

Start with Configuration, calibrate if your hardware has changed, then optimize with real-world logs.

#### Table of Contents

**Configuration**
1. [Loading Files](#stage-1-configuration)
2. [XDF Format Support](#xdf-format-support)
3. [WinOLS KP File Support](#winols-kp-file-support)

**Calibration**
4. [Fueling (KRKTE & Injector Scaling)](#fueling-krkte--injector-scaling)
5. [MAF Scaling (MLHFM)](#maf-scaling-mlhfm)
6. [Pressure to Load (PLSOL)](#pressure-to-load-plsol)
7. [Torque & Load Tables (KFMIOP / KFMIRL)](#torque--load-tables-kfmiop--kfmirl)
8. [Ignition Timing (KFZWOP / KFZW)](#ignition-timing-kfzwop--kfzw)
9. [Throttle Transition (KFVPDKSD)](#throttle-transition-kfvpdksd)
10. [Throttle Body Choke Point (WDKUGDN)](#throttle-body-choke-point-wdkugdn)
11. [Alpha-N Calibration & Diagnostic](#alpha-n-calibration--diagnostic)
12. [Boost PID Linearization (LDRPID)](#boost-pid-linearization-ldrpid)

**Optimization**
13. [Optimizer (Pressure/Load Optimizer)](#stage-3-optimization)

**Reference**
14. [Technical Reference](#technical-reference)

---

# Stage 1: Configuration

ME7Tuner works from a binary file and an XDF definition file. You will need to load these using the ME7Toolbar.

* File -> Open Bin
* XDF -> Select XDF

<img src="/documentation/images/xdf.png" width="800">

See the example binary and XDF in the `example` directory as a starting point.

You will need to tell ME7Tuner what definition you want to use for *all* fields. This is necessary because many XDF
files have multiple definitions for the same map using different units. ***Pay attention to the units!***

ME7Tuner makes the following assumptions about units:

* KRKTE - ms/%
* MLHFM - kg/h
* KFMIOP - %
* KFMIRL - %
* KFZWOP - grad KW
* KFZW - grad KW
* KFVPDKSD - unitless
* WDKUGDN - %
* KFWDKMSN - %
* KFLDRL - %
* KFLDIMX - %
* KFPBRK - unitless (multiplier)
* KFPBRKNW - unitless (multiplier)
* KFPRG - hPa

ME7Tuner automatically filters map definitions base on what is in the editable text box.

<img src="/documentation/images/configuration.png" width="800">

### Log Headers

In some cases ME7Tuner can parse logs automatically to suggest calibrations. There are often many names of the same logged parameter.

You *must* define the headers for the parameters that the log parser uses here.

<img src="/documentation/images/configuration.png" width="800">

## XDF Format Support

ME7Tuner implements the full TunerPro XDF format. This means any ECU binary that has a valid XDF file can be loaded — the parser is not limited to the B5 S4 MBox format.

See [`documentation/me7-xdf-format.md`](documentation/me7-xdf-format.md) for complete technical detail.

### Supported ECUs

| ECU | Application | Notes |
|-----|-------------|-------|
| **ME7 MBox (8D0907551M)** | Audi B5 S4 2.7T | Primary supported ECU — example XDF included |
| **ME7 ABox** | Audi B5 S4 2.7T | Same engine family, compatible maps |
| **ME7 RS4 (8D0907551R)** | Audi B5 RS4 2.7T | Higher boost maps; same VE model |
| **ME7 1.8T (A4/TT/Golf)** | Various 1.8T platforms | Same ME7 software generation; maps compatible |
| **ME7.1** | Later Audi/VW platforms | Compatible when XDF is available |

XDF files for many of these can be found at [files.s4wiki.com/defs/](https://files.s4wiki.com/defs/) and the [Nefarious Motorsports forums](http://nefariousmotorsports.com/forum).

See [`documentation/me7-ecu-compatibility.md`](documentation/me7-ecu-compatibility.md) for ECU compatibility notes.

### Supported XDF Features

| Feature | Support | Notes |
|---------|---------|-------|
| `XDFHEADER` BASEOFFSET | Full | Applied to all addresses at parse time |
| `DEFAULTS` (lsbfirst, signed, float, datasizeinbits) | Full | Inherited as fallbacks when per-axis values are absent |
| `CATEGORY` name map | Full | Available for future UI filtering |
| `CATEGORYMEM` table grouping | Full | Category indices stored per-table |
| `XDFTABLE` (1-D, 2-D, 3-D maps) | Full | All axis combinations supported |
| `XDFCONSTANT` (scalar values) | Full | |
| 8/16/32-bit integer data (signed / unsigned) | Full | |
| 32-bit IEEE-754 float data | Full | |
| Little-endian byte order | Full | Default for all ME7 ECUs |
| Big-endian byte order | Full | Per-axis via `mmedtypeflags` bit 1 or `DEFAULTS lsbfirst="0"` |
| `mmedmajorstridebits` row stride / padding | Full | Negative = virtual axis (LABEL values used) |
| `mmedminorstridebits` element padding | Full | Interleaved data layouts |
| Column-major data layout (`mmedtypeflags` bit 2) | Full | Automatically transposed to row-major |
| Virtual / shared axes (negative stride) | Full | Falls back to `LABEL` breakpoint values |
| `LABEL` axis breakpoint values | Full | Used when axis has no binary address |
| `<decimalpl>` display precision | Parsed | Stored in `AxisDefinition.decimalPl` |
| `<min>` / `<max>` range hints | Parsed | Stored in `AxisDefinition.min/max` |
| `XDFPATCH` code patches | Not supported | No byte-patch UI |
| `XDFFLAG` bitfields | Not supported | No bitfield editor UI |
| `DALINK` / `uniqueid` cross-references | Not needed | All standard ME7 XDFs use `uniqueid="0x0"` |

### Write-Back (Equation Inversion)

When ME7Tuner writes a corrected map back to the binary file, it analytically inverts the XDF's forward equation to convert engineering-unit values back to raw integers. Supported equation forms:

| Forward Equation | Inverse Applied |
|-----------------|----------------|
| `A * X` | `X / A` |
| `A * X + B` | `(X - B) / A` |
| `A * X - B` | `(X + B) / A` |
| `X * A` | `X / A` |
| `X + B` | `X - B` |
| `X - B` | `X + B` |
| `X / A` | `X * A` |
| Anything else | `X` (pass-through — safe for identity equations) |

These cover every equation form produced by the Bosch ME7 TunerPro translators for standard map types.

## WinOLS KP File Support

ME7Tuner includes **hint-mode** support for WinOLS `.kp` ECU definition files.

### What KP files are

WinOLS `.kp` files (EVC GmbH — https://www.evc.de) are **proprietary binary containers**, NOT XML. The format is:

```
[WinOLS binary header]   — WinOLS proprietary metadata
[Embedded ZIP archive]   — standard DEFLATE
  └── intern             — proprietary binary record database
```

The `intern` blob contains map definitions, but the binary layout of axes, dimensions, and scaling factors is
**not publicly documented**. ME7Tuner reverse-engineered the record structure (see [`documentation/me7-kp-format.md`](documentation/me7-kp-format.md))
and can reliably extract map names and binary addresses, but not full axis/scaling data.

### How KP hint mode works

When you load a KP file via `WinOLS → Open KP File...`:

1. ME7Tuner parses the KP file and extracts up to ~90 map name + address pairs
2. When you open any map selection dialog (e.g. *Select KFPBRK*), ME7Tuner:
   - Shows a **hint badge** with the KP-derived description and binary address
   - **Auto-pre-selects** the XDF definition whose address matches the KP address
   - Marks the matched definition with a **KP badge** in the list

This means the map picker is automatically pre-filtered and pre-selected to the most likely correct definition — you don't have to search manually.

### KP vs XDF coverage

| | XDF | KP (hint mode) |
|-|-----|----------------|
| Map definitions | ~393 | ~90 with address, ~62 name-only |
| Axes & dimensions | Full | Not parseable |
| Scaling factors | Full | Not parseable |
| Use case | Primary source of truth | Address cross-reference aid |

**The XDF is always required for binary reading and writing.** The KP file is optional and only provides selection hints.

### Address verification

KP AR addresses and XDF addresses match perfectly for the `8D0907551M` ECU:

| Map | KP address | XDF address |
|-----|-----------|------------|
| KFPBRK | `0x1E3B0` | `0x1E3B0` |
| MLHFM | `0x13974` | `0x13974` |
| KFMIRL | `0x14A1C` | `0x14A1C` |
| KRKTE | `0x1EB44` | `0x1EB44` |
| KFKHFM | `0x10CCE` | `0x10CCE` |

### Why not full KP parsing?

The WinOLS binary format is proprietary and has no public specification. Axis dimensions, element sizes, and scaling
factor offsets are at undocumented positions within each binary record. XDF files for the same ECU contain ~4x more
definitions with full axis/scaling data. See [`documentation/me7-kp-format.md`](documentation/me7-kp-format.md) for the complete reverse-engineering analysis.

KP files available from https://files.s4wiki.com/defs/ can be used alongside the XDF files from the same source.

---

# Stage 2: Calibration

If you've modified engine hardware, the base maps in your BIN no longer match reality. Calibrate in this order — start with a stock binary and work through each section.

*It is critical that you calibrate primary fueling first*.

Fueling is the ***one and only*** known constant to calibrate the MAF. It is highly recommended that you calibrate your fueling with an accurate specification of the fuel injectors.

When the fueling has been calibrated, you can take logs to have ME7Tuner suggest a MAF scaling.

Once the fueling and MAF are calibrated load request, ignition advance and pressure (boost) requested can be calibrated.

For detailed step-by-step instructions, screenshots, and algorithm descriptions for each tool, see the [Calibration Guide](documentation/calibration-guide.md).

## Fueling (KRKTE & Injector Scaling)

The Fueling tab consolidates all fuel-injector-related calibration into one place with two sub-tabs:

### KRKTE (Primary Fueling)

* Read [Primary Fueling](https://s4wiki.com/wiki/Tuning#Primary) first

The first step is to calculate a reasonable value for KRKTE (primary fueling). This is the value that allows the ECU to
determine how much fuel is required to achieve a given AFR (air fuel ratio) based on a requested load/cylinder filling.
It is critical that KRKTE is close to the calculated value. If your KRKTE deviates significantly from the calculated value,
your MAF is likely over/under scaled.

Pay attention to the density of gasoline (Gasoline Grams per Cubic Centimeter). The stock M-box assumes a value of 0.71 g/cc^3,
although the [generally accepted density of gasoline](https://www.aqua-calc.com/page/density-table) is 0.75 g/cc^3. Also consider that ethanol has a density of 0.7893 g/cc^3
so high ethanol blends can be even denser.

Note that the decision to use a fuel density of 0.71 g/cc^3 (versus ~0.75 g/cc^3) will have the effect of under-scaling the MAF (
more fuel will be injected per duty cycle so less airflow will need to be reported from the MAF to compensate). As a result,
the measured engine load (rl_w) will be under-scaled which is key to keeping estimated manifold pressure (ps_w) slightly below
actual pressure (pvdks_w) without making irrational changes to the VE model (KFURL) which converts pressure to load and load
to pressure.

The KRKTE tab of ME7Tuner will help you calculate a value for KRKTE. Simply fill in the constants with the appropriate values.

<img src="/documentation/images/krkte.png" width="800">

### Injector Scaling (KRKTE / TVUB)

When swapping fuel injectors (e.g., upgrading to larger injectors for more fueling headroom), two things need updating in the BIN:

#### KRKTE — Injector Constant (Scalar)

KRKTE is a scalar constant [ms/%] that converts relative fuel mass to injection pulse width. The ME7 injection time formula (me7-raw.txt line 257427) is:

```
te = rk_w × KRKTE + TVUB(ubat)
```

**ME7 does NOT have a 2D injection time map** — there is no "KFTI". The fueling math uses the scalar KRKTE multiplied by the fuel mass request. When you change injectors, KRKTE scales by the flow ratio:

```
KRKTE_new = KRKTE_old × (old_flow / new_flow) × √(P_new / P_old)
```

Where:
- `old_flow / new_flow` = ratio of old to new injector flow rate (cc/min)
- `√(P_new / P_old)` = fuel pressure correction via Bernoulli's principle (= 1.0 if same pressure)

The KRKTE tab computes the absolute value from first principles. The Injector Scaling tab provides a quick ratio-based cross-check.

#### TVUB — Dead Time Table

TVUB is a 1D Kennlinie: battery voltage (V) → dead time (ms). Dead time (Ventilverzugszeit) is the electrical opening delay of the injector solenoid — it does NOT scale with flow rate. Each injector model has a unique dead time curve from the manufacturer's data sheet. If you only have the dead time at 14V, the calculator estimates the full curve using 1/V scaling.

#### KFLF Clarification

KFLF exists in ME7 but is "Lambda map at partial load" — a partial-load AFR target map (RPM × load → lambda). It is **NOT** an injector linearization table. There is no "KFLFW" injector linearization in ME7. ME7 handles minimum pulse width via the TEMIN constant.

#### Usage

1. Open the **Fueling** tab → **Injector Scaling** sub-tab
2. Enter the stock injector flow rate, fuel pressure, and dead time
3. Enter the new injector flow rate, fuel pressure, and dead time
4. Optionally provide a TVUB voltage table (voltage:deadtime pairs) from the injector data sheet
5. Click **Calculate** to get the KRKTE scale factor and TVUB table
6. Apply the results to your BIN

#### Important Notes

- **Changing injectors does NOT require alpha-n (msdk_w) recalibration** — injectors affect fueling, not air measurement
- **Always update KRKTE** (via the KRKTE sub-tab) when changing injectors — KRKTE encodes the injector constant
- **Always reset ECU adaptations** after flashing new injector calibration, then drive with the MAF connected to re-learn fuel trim adaptations
- If the new injectors shift lambda enough to trigger O2 adaptation (KFKHFM), the MAF reading changes, which affects the msndko_w/fkmsdk_w learning — **verify alpha-n accuracy** with the Alpha-N Diagnostic (WDKUGDN tab) after re-learning

## MAF Scaling (MLHFM)

When you are satisfied with KRKTE, you will need to get your MAF scaled to the fuel injectors.

Read [MAF](https://s4wiki.com/wiki/Mass_air_flow)

In any MAFed application it may be necessary to increase the diameter of the MAF housing to extend the range of the sensor (while also reducing resolution) or to change MAF sensors entirely. Incorrect MAF linearization will lead to irrational changes in fueling (KFKHFM/FKKVS), the VE model (KFURL), and load request (LDRXN/KFMIRL). Having an accurate MAF makes tuning considerably simpler.

To scale a MAF we need a source of truth to make corrections against — we can do that in two ways based on fueling. Since we know the size of the injectors, the injector duty cycle and the air-fuel ratio, actual airflow can be calculated and compared against the MAF to make corrections.

### Closed Loop

Uses narrowband O2 sensors and fuel trims (STFT/LTFT) to correct the MAF linearization at part-throttle and idle. This is an iterative process — log, correct, flash, repeat until fuel trims are near 0%.

**What to log:** `nmot`, `fr_w`, `fra_w`, `uhfm_w`, `wdkba`, `B_lr`, `rl_w`

**How much data:** At least 75 minutes of mixed driving (highway, city, parking lot). More data = better corrections. Multiple log files can be loaded at once.

For detailed step-by-step usage, algorithm description, and screenshots see the [Calibration Guide — Closed Loop](documentation/calibration-guide.md#closed-loop-mlhfm).

### Open Loop

Uses a wideband O2 sensor to correct the MAF linearization at WOT. Requires KRKTE and closed loop corrections to be completed first. You need a pre-cat wideband sensor — a tail sniffer likely isn't sufficient.

ME7Tuner matches WOT pulls between ME7Logger and Zeitronix logs by throttle position detection, then correlates data points by RPM within each matched pull. Both logs must contain the same number of pulls.

**What to log (ME7Logger):** `nmot`, `fr_w`, `fra_w`, `uhfm_w`, `mshfm_w`, `wdkba`, `B_lr`, `rl_w`, `lamsbg_w`, `ti_bl`

**What to log (Zeitronix):** AFR

For detailed step-by-step usage, algorithm description, and screenshots see the [Calibration Guide — Open Loop](documentation/calibration-guide.md#open-loop-mlhfm).

## Pressure to Load (PLSOL)

PLSOL is a sanity check tool that converts between pressure, load, airflow, and horsepower. The key insight is that the *only* parameter that affects load is pressure — barometric pressure, intake air temperature, and the pressure-to-load conversion (KFURL) are assumed constant.

This is useful for determining if your hardware can support a given load request. For example, 2.7L of displacement approaching 900hp requires ~3bar (45psi) relative / 4bar (60psi) absolute.

For detailed usage and screenshots see the [Calibration Guide — PLSOL](documentation/calibration-guide.md#plsol---pressure-to-load-conversion).

## Torque & Load Tables (KFMIOP / KFMIRL)

### KFMIOP (Load/Fill to Torque)

KFMIOP describes optimal engine torque as a normalized value (0–100%) relative to the maximum load defined by the MAP sensor limit. When upgrading to a higher-pressure MAP sensor, KFMIOP must be rescaled so that the normalized torque requests remain rational for the new load range.

ME7Tuner analyzes the current KFMIOP to estimate the MAP sensor limit and real-world pressure limit, then generates a rescaled KFMIOP with new axes. The same axes are used for KFZWOP and KFZW so ignition timing scales correctly.

* Read [Torque Monitoring](https://s4wiki.com/wiki/Tuning#Torque_monitoring)

Empirical tuning points:
* Any part of KFMIOP (load/RPM range) that can only be reached above ~60% wped_w is unrestricted and can be raised to keep mimax high such that requested load does not get capped.
* Ensure that mibas remains below miszul to avoid intervention (which you will see in mizsolv) by lowering KFMIOP in areas reachable by actual measured load.

For detailed tuning philosophy, algorithm, and usage see the [Calibration Guide — KFMIOP](documentation/calibration-guide.md#kfmiop-loadfill-to-torque).

### KFMIRL (Torque to Load/Fill)

KFMIRL is the inverse of KFMIOP. It exists as a lookup optimization so the ECU doesn't have to search KFMIOP every time it converts a torque request into a load request. KFMIOP is the input and KFMIRL is the output.

For detailed usage see the [Calibration Guide — KFMIRL](documentation/calibration-guide.md#kfmirl-torque-request-to-loadfill-request).

## Ignition Timing (KFZWOP / KFZW)

If you modified KFMIRL/KFMIOP, you need to modify KFZWOP (optimal ignition timing) and KFZW/2 (ignition timing) to reflect the new load range. ME7Tuner extrapolates the existing timing maps to the new x-axis range generated from KFMIOP.

*Pay attention to the output!* Extrapolation works well for linear functions, but usually doesn't for non-linear functions like ignition advance. Examine the output and make sure it is reasonable before using it — you will probably have to rework it.

For detailed algorithm and usage see the Calibration Guide for [KFZWOP](documentation/calibration-guide.md#kfzwop-optimal-ignition-timing) and [KFZW/2](documentation/calibration-guide.md#kfzw2-ignition-timing).

## Throttle Transition (KFVPDKSD)

In a turbocharged application the throttle is controlled by a combination of the turbo wastegate (N75 valve) and the throttle body valve. KFVPDKSD defines the pressure ratio at which the ECU transitions between throttle-controlled and boost-controlled operation. ME7Tuner parses a directory of logs and determines at what RPM points a given boost level can be achieved.

**What to log:** `nmot`, `wdkba`, `pus_w`, `pvdks_w`

For detailed algorithm and usage see the [Calibration Guide — KFVPDKSD](documentation/calibration-guide.md#kfvpdksd-throttle-transition).

## Throttle Body Choke Point (WDKUGDN)

WDKUGDN is a 1D Kennlinie (`RPM → throttle angle °`) that defines the **choked flow point** of the throttle body at each RPM — the angle at which airflow transitions from throttled to unthrottled.

> **WDKUGDN is NOT an alpha-n calibration map.** It defines a physical property of the throttle body. **Only change WDKUGDN if you physically change the throttle body diameter or engine displacement.** See [Alpha-N Calibration](#alpha-n-calibration--diagnostic) for the correct maps to modify.

Unless you have changed the throttle body or engine displacement, WDKUGDN should not have to be modified.

For the full choked flow algorithm, what WDKUGDN controls, and why changing it for alpha-n is wrong, see the [Calibration Guide — WDKUGDN](documentation/calibration-guide.md#wdkugdn-throttle-body-choke-point).

## Alpha-N Calibration & Diagnostic

Alpha-N (speed-density) mode is when the ECU runs without the MAF sensor. When the MAF is unplugged or fails (`B_ehfm = true`), the ECU switches from `mshfm_w` (MAF-measured airflow) to `msdk_w` (throttle-model-estimated airflow) as the sole load input. If `msdk_w` doesn't match reality, the car runs poorly — wrong fueling, wrong ignition timing, wrong boost targets.

The Alpha-N Diagnostic Tool compares `mshfm_w` against `msdk_w` to assess how well your car will run with the MAF unplugged and identifies exactly which maps need calibrating.

> **Important:** WDKUGDN defines the throttle body choke point. **Do NOT adjust WDKUGDN to fix alpha-n accuracy.** See the BGSRM VE Model section for the correct maps.

### What Actually Needs Calibrating

If `mshfm_w` and `msdk_w` diverge after hardware changes, calibrate in priority order:

| Priority | Map/Parameter | When to Change |
|----------|--------------|----------------|
| 1 | **KFMSNWDK** (throttle body flow map) | Changed throttle body or intake manifold |
| 2 | **KFURL / KFPBRK / KFPRG** (VE model) | Changed cams, head work, or port work |
| 3 | **Adaptation reset** (msndko_w, fkmsdk_w) | After ANY map changes — let the ECU re-learn |
| 4 | **KUMSRL** (mass flow conversion) | Changed engine displacement |

**Fuel injector changes do NOT require alpha-n recalibration.** Injectors affect fueling, not air measurement. However, reset adaptations after injector changes to avoid stale learned values.

### Error Type Classification

The diagnostic classifies the dominant error between `mshfm_w` and `msdk_w`:

| Error Type | Pattern | Fix |
|-----------|---------|-----|
| **ADDITIVE** | Constant offset regardless of airflow | Reset adaptations; check vacuum leaks |
| **MULTIPLICATIVE** | Error scales with airflow | Reset adaptations; re-calibrate KFMSNWDK |
| **RPM_DEPENDENT** | Varies with RPM, not consistently with load | Use Optimizer for per-RPM KFURL/KFPBRK correction |
| **MIXED** | Combination of above | Reset adaptations first, then investigate per-RPM |

### Usage

**What to log:** `nmot`, `mshfm_w`, `msdk_w` (required); `wdkba`, `pvdks_w`, `pus_w`, `rl_w` (recommended)

Log across various RPM and load conditions (idle, cruise, part-throttle, WOT) with the MAF connected.

For full background on main vs side load signals, the BGSRM VE model, the 6 calibratable components, and step-by-step usage with screenshots, see the [Calibration Guide — Alpha-N](documentation/calibration-guide.md#alpha-n-calibration--diagnostic-tool).

For detailed technical documentation including me7-raw.txt line references, see [`documentation/me7-alpha-n-calibration.md`](documentation/me7-alpha-n-calibration.md).

## Boost PID Linearization (LDRPID)

Provides a feed-forward (pre-control) factor to the existing PID boost controller. Highly recommended — the linearization process is a lot of work by hand, but ME7Tuner can parse millions of data points to produce the linearization table.

Read [Actual pre-control in LDRPID](http://nefariousmotorsports.com/forum/index.php?topic=12352.0title=)

**What to log:** `nmot`, `pvdks_w`, `pus_w`, `wdkba`, `ldtvm`, `gangi`

Get as many WOT pulls as possible across the full RPM range. Put all logs in a single directory and load them in ME7Tuner. The linearized duty cycle will be output in KFLDRL, and a new KFLDIMX with x-axis will be estimated from the linearized boost table.

For the full algorithm and step-by-step usage see the [Calibration Guide — LDRPID](documentation/calibration-guide.md#ldrpid-feed-forward-pid).

---

# Stage 3: Optimization

The Optimizer is a suggestion engine that analyzes WOT (Wide Open Throttle) logs and recommends corrections to the boost control and volumetric efficiency maps so that **actual pressure tracks pssol** (requested pressure) and **actual load tracks LDRXN** (maximum specified load).

The core philosophy is that ME7's internal physical model — converting between pressure and load via KFURL and KFPBRK — is mathematically sound. If the base maps are calibrated correctly, the ECU's requested values should match reality (barring mechanical limitations such as turbo overspooling, knock limiting, boost leaks, etc.). When there is a discrepancy, the Optimizer identifies exactly *where* the error is and suggests specific map changes to fix it.

For detailed documentation on how the Optimizer works internally, see [`documentation/optimizer-architecture.md`](documentation/optimizer-architecture.md) and [`documentation/optimizer-algorithms.md`](documentation/optimizer-algorithms.md).

## Prerequisites

Before using the Optimizer, you should have already:

1. **Calibrated KRKTE and TVUB** (Fueling tab) so the ECU knows the correct fuel injector constant and dead times
2. **Scaled MLHFM** (MAF sensor) using the Closed Loop and Open Loop tabs so that fuel trims are near 0% and actual AFR matches requested AFR
3. **Defined KFMIRL/KFMIOP** (load and torque tables) scaled for your MAP sensor limits
4. **Calibrated LDRPID** (feed-forward PID / KFLDRL) with at least a rough baseline

The Optimizer refines the boost control and VE model *after* these foundations are in place.

## Configuration

### Map Definitions

In the **Configuration** tab, you must select map definitions for the following maps. The Optimizer uses these to read the current map values from the BIN and to write corrections back:

| Map | Purpose | Required? |
|-----|---------|-----------|
| **KFLDRL** | Wastegate base duty cycle (pre-control) | Yes, for boost corrections |
| **KFLDIMX** | PID I-limiter (feed-forward cap) | Yes, for boost corrections |
| **KFPBRK** | Volumetric efficiency multiplier (cams off) | Yes, for VE corrections |
| **KFPBRKNW** | Volumetric efficiency multiplier (cams on) | Optional (displayed for reference) |

ME7Tuner makes the following assumptions about units for these maps:

* KFLDRL - %
* KFLDIMX - %
* KFPBRK - unitless (multiplier)
* KFPBRKNW - unitless (multiplier)

### Log Headers

In the **Configuration** tab under Log Headers, ensure the following headers are defined correctly. These must match the column names in your ME7Logger CSV files:

| Parameter | Default Header | Description |
|-----------|---------------|-------------|
| RPM | `nmot` | Engine RPM |
| Throttle Plate Angle | `wdkba` | Throttle position (degrees) |
| Wastegate Duty Cycle | `ldtvm` | Total wastegate duty cycle (%) |
| Barometric Pressure | `pus_w` | Ambient barometric pressure (mbar) |
| Absolute Pressure | `pvdks_w` | Actual absolute manifold pressure (mbar) |
| Requested Pressure | `pssol_w` | ECU's requested manifold pressure (mbar) |
| Requested Load | `rlsol_w` | ECU's requested load (%) |
| Engine Load | `rl_w` | Actual measured engine load (%) |
| Actual Load | `rl` | Actual load (optional, alternative to rl_w) |

## How It Works

The Optimizer operates in three phases:

### Phase 1: Boost Control (pssol vs. pvdks_w → KFLDRL / KFLDIMX)

Before load can be accurate, the turbo must hit the pressure the ECU is requesting. If `pvdks_w` (actual pressure) does not equal `pssol_w` (requested pressure), the wastegate pre-control (KFLDRL / KFLDIMX) needs adjustment.

**Algorithm:**

1. Filter WOT data (throttle angle ≥ minimum threshold, default 80°)
2. For each RPM breakpoint in KFLDRL, find log rows where the PID successfully matched actual boost to requested boost (within the MAP tolerance, default ±30 mbar)
3. At those stable-boost data points, capture the average WGDC (`ldtvm`) the ECU was actually outputting
4. Suggest replacing each KFLDRL cell with that observed WGDC
5. Derive KFLDIMX by multiplying the suggested KFLDRL values by (1 + overhead%), default 108%, giving the PID room to operate

**Interpretation:** "At 4000 RPM, you requested 2200 mbar. To hit that, the ECU ultimately used 65% WGDC. The suggested KFLDRL at 4000 RPM is 65%."

### Phase 2: VE Model (rl vs. rlsol → KFPBRK corrections)

Once Phase 1 is complete (actual pressure tracks pssol), the Optimizer evaluates whether the load is correct. If your MAF is properly scaled but actual load (`rl_w`) consistently misses requested load (`rlsol_w`), the ECU's mathematical conversion between pressure and load needs adjustment via KFPBRK.

**Algorithm:**

1. Prerequisite: Only analyze data points where boost is on-target (`|pvdks_w − pssol_w| ≤ tolerance`)
2. At each RPM breakpoint, compute the load ratio: `requestedLoad / actualLoad`
3. Multiply the current KFPBRK cell values by this ratio to produce the suggested KFPBRK

**Interpretation:** A ratio of 1.05 means the ECU needs to request 5% more pressure to achieve the target load — KFPBRK is scaled up by 5% at that RPM.

### Phase 3: Intervention Check (Torque Limiters)

Sometimes LDRXN won't be reached because a torque monitor or intervention is secretly capping the request before it ever reaches the boost controller.

**Algorithm:**

1. Compare `rlsol_w` (the final load request) against the configured LDRXN target
2. If `rlsol_w < LDRXN × 0.95` during WOT, flag a **Torque Intervention Warning**
3. If `pvdks_w` consistently falls more than 50 mbar below `pssol_w`, flag a **Boost Target Not Reached** warning

## Usage

### Step 1: Configure Maps and Parameters

1. Go to the **Configuration** tab
2. Select map definitions for KFLDRL, KFLDIMX, KFPBRK (and optionally KFPBRKNW)
3. Ensure the log header definitions include `pssol_w`, `rlsol_w`, and `rl_w`

### Step 2: Collect WOT Logs

Log the following parameters with ME7Logger during WOT pulls:

* `nmot` (RPM)
* `wdkba` (Throttle Plate Angle)
* `ldtvm` (Wastegate Duty Cycle)
* `pus_w` (Barometric Pressure)
* `pvdks_w` (Actual Absolute Manifold Pressure)
* `pssol_w` (Requested Pressure)
* `rlsol_w` (Requested Load)
* `rl_w` (Actual Engine Load)

Get WOT pulls across the full RPM range. More data points produce better suggestions.

### Step 3: Load and Analyze

1. Go to the **Optimizer** tab
2. Adjust the parameters if needed:
   * **MAP Tolerance (mbar):** How close actual boost must be to requested boost for a data point to count as "on-target" (default: 30 mbar)
   * **LDRXN Target (%):** Your maximum specified load target for the intervention check (default: 191%)
   * **KFLDIMX Overhead (%):** How much headroom above KFLDRL to give the PID I-limiter (default: 8%)
   * **Min Throttle Angle:** Minimum throttle angle to qualify as WOT data (default: 80°)
3. Click **"Load ME7 Log File"** for a single log, or **"Load ME7 Log Directory"** to process multiple logs at once
4. Review the results across the four tabs:

### Step 4: Review Results

#### Boost Control Tab

Shows the current KFLDRL and KFLDIMX maps side-by-side with the suggested corrections. If the suggestions look reasonable, click **"Write KFLDRL"** or **"Write KFLDIMX"** to write directly to the BIN file.

#### VE Model Tab

Shows the current KFPBRK map alongside the suggested KFPBRK with load ratio corrections applied. Click **"Write KFPBRK"** to write to the BIN.

KFPBRKNW (variable cam timing active) is displayed for reference. The Optimizer currently applies corrections to KFPBRK only because cam state (`nw_w`) is not typically logged. If your vehicle has active variable cam timing, the same ratio approach can be applied manually to KFPBRKNW.

#### Charts Tab

Visual comparison of requested vs. actual values:

* **Pressure vs RPM:** Scatter plot of pssol (requested) and pvdks_w (actual) over RPM — shows how well the boost controller is tracking
* **Pressure Error vs RPM:** Shows `pssol − pvdks_w` at each data point — positive values mean the ECU is requesting more boost than is being delivered
* **Load vs RPM:** Scatter plot of rlsol (requested) and rl_w (actual) over RPM — shows how well the VE model is tracking
* **Load Ratio vs RPM:** Shows `rlsol / rl_w` at each data point — 1.0 is perfect, values above 1.0 mean the ECU expected more load than was achieved

#### Warnings Tab

Displays any detected issues:

* **Torque Intervention Detected:** `rlsol_w` is significantly below the LDRXN target, meaning a torque limiter (KFMIOP, KFMIZUFIL, etc.) is capping the load request before it reaches the boost controller. You need to scale your torque maps higher.
* **Boost Target Not Reached:** Actual pressure is consistently below requested pressure. This may indicate a mechanical limitation (turbo spool, wastegate, boost leak) or an incorrect KFLDRL base duty cycle.

A summary statistics panel shows data point counts, RPM range, average/max pressure error, average load ratio, and average WGDC.

### Step 5: Iterate

The Optimizer is designed for iterative use:

1. **First pass:** Fix boost control (Phase 1). Write the suggested KFLDRL and KFLDIMX, then take new logs.
2. **Second pass:** With boost on-target, fix the VE model (Phase 2). Write the suggested KFPBRK, then take new logs.
3. **Verify:** On the final pass, both pressure and load charts should show tight tracking between requested and actual values. Warnings should be clear.

If the maps are calibrated correctly, `pssol` should match `pvdks_w` and `rlsol` should match `rl_w` — the ECU's physical model "just works."

### Sensor Voltage Saturation Detection

All ME7 analog sensors output 0–5 V. When a tuned engine pushes a sensor beyond its measurement range, the voltage clips at or near 5 V and the ECU cannot measure the real value. The Optimizer detects this automatically.

#### Detected Sensors

| Sensor | Log Signal | What Saturates | Stock Max | Upgrade Path |
|--------|-----------|----------------|-----------|--------------|
| **MAF (HFM5)** | `uhfm_w` | Airflow exceeds MLHFM top voltage bin | ~4.96 V (~370 g/s) | Rescale MLHFM for larger MAF housing (e.g., 80mm → 85mm) |
| **MAP (3-bar)** | `pvdks_w` | Boost pressure exceeds sensor ceiling | ~2550 mbar | Upgrade to 4-bar MAP sensor, update KFVPDKSD/KFVPDKSE |
| **MAP (4-bar)** | `pvdks_w` | Boost pressure exceeds sensor ceiling | ~3500 mbar | Upgrade to 5-bar MAP sensor, update KFVPDKSD/KFVPDKSE |
| **MAP (5-bar)** | `pvdks_w` | Boost pressure exceeds sensor ceiling | ~4500 mbar | 6-bar or dual-sensor setup |

#### How It Works

1. **MAF Voltage Clipping:** If `uhfm_w` is logged, the Optimizer checks whether ≥10% of WOT samples are at or above 4.8 V. When saturated, `mshfm_w` (airflow in g/s) plateaus — any airflow beyond the top MLHFM bin is invisible. This means all downstream calculations (load, fueling, VE model corrections) at high-RPM/high-load are based on incorrect airflow readings.

2. **MAP Sensor Type Auto-Detection:** The Optimizer classifies the installed MAP sensor type (3-bar, 4-bar, 5-bar, etc.) from the observed maximum `pvdks_w` pressure, then checks whether samples are plateauing near that sensor's ceiling.

3. **Data Reliability Warning:** When any sensor is saturated, the Optimizer marks solver suggestions at affected operating points as potentially unreliable. A warning banner appears in the Mechanical Limits card explaining which solvers are affected and why.

#### Recommended Logging

For optimal sensor saturation detection, include `uhfm_w` (MAF voltage) in your ME7Logger configuration alongside the standard optimizer channels. The MAP sensor saturation is detected from `pvdks_w` which is already a required channel.

---

# Technical Reference

Detailed documentation for deep dives into ME7 internals:

| Document | Description |
|----------|-------------|
| [`me7-boost-control.md`](documentation/me7-boost-control.md) | Signal path reference (torque → load → pressure → boost) with me7-raw.txt line citations |
| [`me7-maps-reference.md`](documentation/me7-maps-reference.md) | Complete catalog of ME7 maps, constants, and log signals relevant to the Optimizer |
| [`me7-alpha-n-calibration.md`](documentation/me7-alpha-n-calibration.md) | Alpha-N deep dive — what actually needs calibrating for correct MAF-off operation |
| [`me7-xdf-format.md`](documentation/me7-xdf-format.md) | XDF format details — field meanings, parser/reader/writer pipeline |
| [`me7-kp-format.md`](documentation/me7-kp-format.md) | WinOLS KP format reverse-engineering analysis |
| [`me7-ecu-compatibility.md`](documentation/me7-ecu-compatibility.md) | Supported ME7/ME7.x ECU variants and how to add new ECUs |
| [`DAMOS.md`](documentation/DAMOS.md) | DAMOS 2 format reference derived from ME7.1 ECU files |
| [`optimizer-architecture.md`](documentation/optimizer-architecture.md) | How the Optimizer works and why it's built that way |
| [`optimizer-algorithms.md`](documentation/optimizer-algorithms.md) | Detailed algorithm descriptions for each detector, solver, and suggestion method |
| [`calibration-guide.md`](documentation/calibration-guide.md) | Step-by-step calibration usage with screenshots for every tool |
