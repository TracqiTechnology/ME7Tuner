<p align="center">
  <img src="/documentation/images/banner.png" alt="TracQi ME7Tuner">
</p>

<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License"></a>
  <a href="https://github.com/TracqiTechnology/ME7Tuner/releases/latest"><img src="https://img.shields.io/github/v/release/TracqiTechnology/ME7Tuner?color=green" alt="Release"></a>
  <a href="https://github.com/TracqiTechnology/ME7Tuner/actions/workflows/release.yml"><img src="https://github.com/TracqiTechnology/ME7Tuner/actions/workflows/release.yml/badge.svg?branch=master" alt="Build"></a>
  <img src="https://img.shields.io/github/downloads/TracqiTechnology/ME7Tuner/total?color=brightgreen" alt="Downloads">
  <img src="https://img.shields.io/badge/Java-17+-orange.svg" alt="Java 17+">
  <img src="https://img.shields.io/badge/Kotlin-Compose_Desktop-7F52FF.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/Platform-macOS_|_Windows_|_Linux-lightgrey.svg" alt="Platform">
</p>

ME7Tuner is a calibration and optimization tool for Bosch ME7 and MED17 ECUs. It provides calculators for fueling, injector scaling, torque/load tables, ignition timing, and boost control — plus a log-analysis Optimizer that diagnoses and corrects boost control and volumetric efficiency errors from real-world data. MED17 support adds dual injection (port + direct) calibration and ScorpionEFI log parsing for the Audi RS3/TTRS 2.5T and related platforms.

ME7Tuner supports any ME7 or MED17 variant that has a TunerPro XDF definition file. Bundled profiles are included for the Audi B5 S4/RS4 2.7T (ME7), B5/B6 A4 1.8T, Audi TT 1.8T, VW Golf/Jetta 1.8T, and Audi RS3/TTRS 2.5T TFSI (MED17.1.62) platforms.

<img src="/documentation/images/me7Tuner.png" width="800">

# Warning

ME7Tuner is free software written by some guy on the internet. ***ME7Tuner comes with no warranty.*** Use at your own risk.

It is a certainty that ME7Tuner will produce garbage outputs at some point and you will damage your engine if you do not know what you are doing. ME7Tuner is software that *helps* you calibrate your engine. It does not calibrate your engine for you. It is not a replacement for knowledge of how to calibrate an engine. If you send 25 psi into a motor that can handle 15 psi because you didn't read the output, that's between you and your engine builder.

## Installation

ME7Tuner ships as a **native application** — no JRE required:

| Platform | Format | Notes |
|----------|--------|-------|
| **macOS** | `.dmg` | Apple Silicon native. Intel Macs run via Rosetta 2. |
| **Windows** | `.msi` | Double-click install. No JRE required. |
| **Linux** | `.deb` + `.tar.gz` | Debian package or portable archive. |
| **Cross-platform** | `.jar` | For the traditionalists. Requires Java 17+. |

Your buddy who's been "meaning to install Java" for three years can finally just run the DMG.

Download the latest release [here](https://github.com/TracqiTechnology/ME7Tuner/releases/latest).

> **JAR users:** You will need [Java 17+](https://www.oracle.com/java/technologies/downloads/) installed. Once you have it, double-click the JAR and you're off.

# So, Do I Actually Need This?

ME7Tuner has two major workflows — **Calibration** and **Optimization** — and who needs each is different.

### Optimizer: Yes, you probably need it

The Optimizer analyzes WOT logs and corrects your boost control (KFLDRL/KFLDIMX) and volumetric efficiency model (KFPBRK) so that actual pressure tracks requested pressure and actual load tracks requested load. This is useful at **any power level** — even a completely stock K03 car benefits from having an accurate VE model and properly linearized wastegate duty cycle. If your car runs ME7 and you datalog, the Optimizer can improve your tune.

### Calibration: Only if you've changed hardware

The Calibration tools (fueling, MAF scaling, torque/load tables, ignition timing, throttle transition) are for engines where the base maps no longer match reality — typically because you've upgraded turbos, injectors, MAF housings, or increased the MAP sensor limit. For most applications, the stock M-box calibration is sufficient to support K03 and basic K04 configurations without recalibration.

In general you need the Calibration workflow if you need to request more than 191% load on an M-Box. The following hardware reference should give you a good estimate of what you need to hit a given power goal and how much calibration pain is in your future.

##### Stock MAP Limit

The stock MAP limit is the brick wall. A stock M-box has *just* enough overhead to support K04's within their optimal efficiency range, and not a millibar more.

* 2.5bar absolute (~22.45 psi relative)

Read [MAP Sensor](https://s4wiki.com/wiki/Manifold_air_pressure)
Read [5120 Hack](http://nefariousmotorsports.com/forum/index.php?topic=3027.0)

Unless you're planning on making more than 2.5 bar absolute (~22.45 psi relative) of pressure, you don't need the Calibration workflow. You may still benefit from the [Optimizer](#stage-3-optimization).

##### Turbo Airflow

<img src="/documentation/images/charts/turbo_airflow.png" width="800">

Note: Remember to multiply by the number of turbos. The 2.7T has two turbos. (Yes, people forget this. No, we won't stop reminding you.)

Unless you are maxing your K04's or running a larger turbo frame, you don't need the Calibration workflow. The [Optimizer](#stage-3-optimization) can still improve your tune at any power level.

##### MAF Airflow

<img src="/documentation/images/charts/maf_airflow.png" width="800">

Read [MAF Sensor](https://s4wiki.com/wiki/Mass_air_flow)

Unless you are maxing the stock MAF or running a larger housing, you don't need MAF recalibration. The [Optimizer](#stage-3-optimization) can still improve your tune at any power level.

##### Fuel for Airflow (10:1 AFR)

<img src="/documentation/images/charts/fuel_demand.png" width="800">

Note: Remember to multiply air by the number of turbos and divide fuel by the number of fuel injectors. The 2.7T has 6 fuel injectors.

##### Theoretical fuel injector size for a V6 bi-turbo configuration

<img src="/documentation/images/charts/injector_size.png" width="800">

Read [Fuel Injectors](https://s4wiki.com/wiki/Fuel_injectors)

##### Theoretical load for a 2.7l V6 configuration

<img src="/documentation/images/charts/load_hp.png" width="800">

Note that a stock M-box has a maximum load request of 191%, but can be increased with the stock MAP sensor to ~215%.

### MED17: Same story, different ECU

If you're running MED17 — RS3, TTRS, or any of the EA855 EVO / EA888 Gen3 / 4.0T / 5.2 V10 DS1 cars — ME7Tuner has you covered. The workflow is the same: Calibration for hardware changes, Optimization for boost control and VE tuning. MED17 adds dual injection (port + direct) which means two injector banks to calibrate, two KRKTE scalars to compute, and a fuel split to manage. If that sounds like twice the work, it is. ME7Tuner does the math so you don't have to.

The 2.5T cars make insane power very easily and get into high load territory fast. If you're running aftermarket turbos, you'll need to rescale KFMIOP/KFMIRL (called KFLMIOP/KFLMIRL in MED17) and redo the entire PID setup (KFLDRL + every table in the PID chain) because the stock PID was linearized for a turbo that doesn't exist on your car anymore.

**What's different in MED17 vs ME7:**

| Feature | ME7 | MED17 |
|---------|-----|-------|
| Injection | Single bank | Dual (port + direct) |
| MAF Scaling | MLHFM linearization curve | Not applicable (adaptive VE model via `fupsrl_w`) |
| VE Model | KFURL/KFPBRK (static maps) | Adaptive (`fupsrls_w` / `pbrint_w`) |
| Throttle Model | KFVPDKSD + WDKUGDN | Different architecture (not calibratable here) |
| Alpha-N | msdk_w vs mshfm_w diagnostic | Not applicable |
| Torque Tables | KFMIOP / KFMIRL | KFLMIOP / KFLMIRL (same math, different names) |
| Boost PID | KFLDRL / KFLDIMX | Same maps, same PID — but non-linear response with aftermarket hardware |
| Log Format | ME7Logger CSV | ScorpionEFI CSV |

ME7Tuner automatically shows only the tabs relevant to your platform. Switch between ME7 and MED17 in the Configuration tab.

# How ME7 Actually Works (Read This First)

Everything in ME7 revolves around requested load (or cylinder fill). Understand this and everything else makes sense. Skip it and you'll spend weeks chasing symptoms.

* Read [Engine load](https://s4wiki.com/wiki/Load)

Here's the signal chain: the driver pushes the accelerator pedal, which makes a torque request. That torque request gets mapped to a load request. ME7 then calculates how much pressure (boost) is required to achieve that load — and that calculation depends heavily on hardware (engine, turbo, intercooler) and weather (cold, dry air is denser than hot, humid air). Tuning ME7 means calibrating the various maps so this model accurately reflects *your* hardware in *your* conditions. If the model is wrong, ME7 knows something doesn't add up and protects the engine by pulling power at various levels of intervention.

Here's the part people miss: **no amount of hardware modifications will increase power if actual load already equals or exceeds requested load.** Bigger turbo, bigger intercooler, better exhaust — none of it matters if the ECU is already capping output. ME7 uses interventions to *decrease* actual load to match the request. You must calibrate the tune to *request more load* before you'll see more power.

ME7Tuner provides the calculations that let you get airflow, pressure, and load measurements right — so the model works instead of fighting you.

## How MED17 Works (Same Idea, Modern Execution)

MED17 follows the same torque-based architecture as ME7 — torque request → load request → pressure target → boost control. The driver model, torque monitoring, and intervention logic are conceptually identical. The differences are in the details: MED17 uses an adaptive volumetric efficiency model instead of the static KFURL/KFPBRK maps, it has dual injection (port + direct) with separate injector characterization for each bank, and the logging ecosystem is ScorpionEFI instead of ME7Logger.

If you understand ME7's signal chain, you understand MED17's. The map names change (KFMIOP → KFLMIOP, KFMIRL → KFLMIRL), the log signal names change (`pvdks_w` → `psrg_w`, `pssol_w` → `pvds_w`), but the physics doesn't.

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
5. [Dual Injection (MED17 Only)](#dual-injection-med17-only)
6. [MAF Scaling (MLHFM)](#maf-scaling-mlhfm)
7. [Pressure to Load (PLSOL)](#pressure-to-load-plsol)
8. [Torque & Load Tables (KFMIOP / KFMIRL)](#torque--load-tables-kfmiop--kfmirl)
9. [Ignition Timing (KFZWOP / KFZW)](#ignition-timing-kfzwop--kfzw)
10. [Throttle Transition (KFVPDKSD)](#throttle-transition-kfvpdksd)
11. [Throttle Body Choke Point (WDKUGDN)](#throttle-body-choke-point-wdkugdn)
12. [Alpha-N Calibration & Diagnostic](#alpha-n-calibration--diagnostic)
13. [Boost PID Linearization (LDRPID)](#boost-pid-linearization-ldrpid)

**Optimization**
14. [Optimizer (Pressure/Load Optimizer)](#stage-3-optimization)

**Reference**
15. [Technical Reference](#technical-reference)

---

# Stage 1: Configuration

ME7Tuner works from a binary file and an XDF definition file. Load these using the menu bar:

* **File > Open Bin...** — select your ME7 binary file
* **XDF > Select XDF...** — select the matching XDF definition file

See the example binary and XDF in the `example` directory as a starting point.

### Platform Selection

ME7Tuner supports both ME7 and MED17 ECU platforms. Select your platform in the Configuration tab — the UI will automatically show only the calibration tools that apply to your ECU. ME7 shows MAF scaling, throttle transition, and alpha-N tools. MED17 shows dual injection calibration. Shared tools (fueling, torque/load tables, ignition timing, boost PID, PLSOL, and the Optimizer) appear on both.

When you switch platforms, map definitions are filtered to match. You won't accidentally pick an ME7 map definition when working on a MED17 binary.

You will need to tell ME7Tuner what definition you want to use for *all* fields. This is necessary because many XDF files have multiple definitions for the same map using different units. ***Pay attention to the units!*** Seriously — picking the wrong unit definition is the single most common setup mistake, and ME7Tuner cannot save you from it.

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

**MED17 unit assumptions:**

* KRKTE (Port) - ms/%
* KRKTE (Direct) - ms/%
* TVUB (Port) - ms
* KFLMIOP - %
* KFLMIRL - %
* KFZWOP - grad KW
* KFZW - grad KW
* KFLDRL - %
* KFLDIMX - %

ME7Tuner automatically filters map definitions based on what is in the editable text box.

<img src="/documentation/images/configuration.png" width="800">

### Log Headers

Some tools can parse logs automatically to suggest calibrations. The catch: there are often many names for the same logged parameter, and ME7Tuner can't guess which one you're using.

You *must* define the headers for the parameters that the log parser uses here.

<img src="/documentation/images/configuration.png" width="800">

#### MED17 Log Headers (ScorpionEFI)

MED17 cars typically use ScorpionEFI for logging. The signal names differ from ME7Logger — configure these in the Log Headers section:

| Parameter | ScorpionEFI Header | ME7 Equivalent | Description |
|-----------|-------------------|----------------|-------------|
| RPM | `nmot_w` | `nmot` | Engine speed |
| Throttle Plate Angle | `wdkba` | `wdkba` | Throttle position (degrees) |
| Wastegate Duty Cycle | `tvldste_w` | `ldtvm` | Final WGDC output (%) |
| Barometric Pressure | `pu_w` | `pus_w` | Ambient barometric pressure (mbar) |
| Absolute Pressure | `psrg_w` | `pvdks_w` | Actual absolute manifold pressure (mbar) |
| Requested Pressure | `pvds_w` | `pssol_w` | ECU's requested manifold pressure (mbar) |
| Requested Load | `rlsol_w` | `rlsol_w` | ECU's requested load (%) |
| Engine Load | `rl_w` | `rl_w` | Actual measured engine load (%) |
| Live VE | `fupsrls_w` | — | Live volumetric efficiency (MED17 only) |
| Gear | `gangi` | `gangi` | Current gear |

ME7Tuner's adapter layer maps these automatically — configure the headers once in the Configuration tab and the parsers handle the translation.

<img src="/documentation/images/med17/configuration_med17.png" width="800">

## XDF Format Support

ME7Tuner implements the **full** TunerPro XDF format. This means any ECU binary that has a valid XDF file can be loaded — the parser is not limited to the B5 S4 MBox format. We reverse-engineered every field, every flag, every stride mode. The XDF spec is not publicly documented, so we had to figure it out the hard way.

See [`documentation/me7-xdf-format.md`](technical/me7/me7-xdf-format.md) for the complete technical deep-dive.

### Supported ECUs

| ECU | Application | Notes |
|-----|-------------|-------|
| **ME7 MBox (8D0907551M)** | Audi B5 S4 2.7T | Primary supported ECU — example XDF included |
| **ME7 ABox** | Audi B5 S4 2.7T | Same engine family, compatible maps |
| **ME7 RS4 (8D0907551R)** | Audi B5 RS4 2.7T | Higher boost maps; same VE model |
| **ME7 1.8T (A4/TT/Golf)** | Various 1.8T platforms | Same ME7 software generation; maps compatible |
| **ME7.1** | Later Audi/VW platforms | Compatible when XDF is available |
| **MED17.1.62 (8S0907404x)** | Audi RS3 / TTRS 2.5T TFSI (EA855 EVO) | Full support — dual injection, ScorpionEFI logs |
| **MED17.1 (4.0T)** | Audi RS6/RS7/S6/S7 4.0T TFSI | Compatible when XDF is available |
| **MED17.1 (5.2 V10)** | Audi R8 / Lamborghini Huracán 5.2 V10 | Compatible when XDF is available |

XDF files for many of these can be found at [files.s4wiki.com/defs/](https://files.s4wiki.com/defs/) and the [Nefarious Motorsports forums](http://nefariousmotorsports.com/forum).

See [`documentation/me7-ecu-compatibility.md`](technical/me7/me7-ecu-compatibility.md) for ECU compatibility notes.

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

When ME7Tuner writes a corrected map back to the binary, it analytically inverts the XDF's forward equation to convert engineering-unit values back to raw integers. No GraalVM round-trip, no numerical solver — just algebra:

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

ME7Tuner includes **hint-mode** support for WinOLS `.kp` ECU definition files. We reverse-engineered the proprietary binary format to make this work. You're welcome.

### What KP files are

WinOLS `.kp` files (EVC GmbH — https://www.evc.de) are **proprietary binary containers**, NOT XML. The format is:

```
[WinOLS binary header]   — WinOLS proprietary metadata
[Embedded ZIP archive]   — standard DEFLATE
  └── intern             — proprietary binary record database
```

The `intern` blob contains map definitions, but the binary layout of axes, dimensions, and scaling factors is **not publicly documented**. ME7Tuner reverse-engineered the record structure (see [`documentation/me7-kp-format.md`](technical/me7/me7-kp-format.md)) and can reliably extract map names and binary addresses, but not full axis/scaling data.

### How KP hint mode works

When you load a KP file via `WinOLS → Open KP File...`:

1. ME7Tuner parses the KP file and extracts up to ~90 map name + address pairs
2. When you open any map selection dialog (e.g. *Select KFPBRK*), ME7Tuner:
   - Shows a **hint badge** with the KP-derived description and binary address
   - **Auto-pre-selects** the XDF definition whose address matches the KP address
   - Marks the matched definition with a **KP badge** in the list

The map picker is automatically pre-filtered and pre-selected to the most likely correct definition — no more scrolling through 393 XDF entries hunting for the right one.

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

The WinOLS binary format is proprietary and has no public specification. Axis dimensions, element sizes, and scaling factor offsets are at undocumented positions within each binary record. XDF files for the same ECU contain ~4x more definitions with full axis/scaling data. See [`documentation/me7-kp-format.md`](technical/me7/me7-kp-format.md) for the complete reverse-engineering analysis.

KP files available from https://files.s4wiki.com/defs/ can be used alongside the XDF files from the same source.

---

# Stage 2: Calibration

If you've modified engine hardware, the base maps in your BIN no longer match reality. This is where things get real. Calibrate in this order — start with a stock binary and work through each section.

*It is critical that you calibrate primary fueling first.* This is not a suggestion. This is not a "best practice." If you skip this, everything downstream is built on a lie.

Fueling is the ***one and only*** known constant to calibrate the MAF. It is highly recommended that you calibrate your fueling with an accurate specification of the fuel injectors. Once fueling is calibrated, you can take logs and have ME7Tuner suggest a MAF scaling. Once both fueling and MAF are calibrated, load request, ignition advance, and pressure (boost) can be calibrated.

> **MED17 users:** The same calibration order applies. Fueling first (both port and direct injectors), then torque/load tables, then boost PID. The tools that don't apply to MED17 (MAF scaling, throttle transition, WDKUGDN, alpha-N) are automatically hidden when MED17 is selected.

For detailed step-by-step instructions, screenshots, and algorithm descriptions for each tool, see the [Calibration Guide](documentation/calibration-guide.md).

## Fueling (KRKTE & Injector Scaling)

The Fueling tab consolidates all fuel-injector-related calibration into one place with two sub-tabs:

### KRKTE (Primary Fueling)

* Read [Primary Fueling](https://s4wiki.com/wiki/Tuning#Primary) first

The first step is to calculate a reasonable value for KRKTE (primary fueling). This is the value that allows the ECU to determine how much fuel is required to achieve a given AFR (air fuel ratio) based on a requested load/cylinder filling. It is critical that KRKTE is close to the calculated value. If your KRKTE deviates significantly from the calculated value, your MAF is likely over/under scaled.

Pay attention to the density of gasoline (Gasoline Grams per Cubic Centimeter). The stock M-box assumes a value of 0.71 g/cc^3, although the [generally accepted density of gasoline](https://www.aqua-calc.com/page/density-table) is 0.75 g/cc^3. Also consider that ethanol has a density of 0.7893 g/cc^3 so high ethanol blends can be even denser.

Note that the decision to use a fuel density of 0.71 g/cc^3 (versus ~0.75 g/cc^3) will have the effect of under-scaling the MAF (more fuel will be injected per duty cycle so less airflow will need to be reported from the MAF to compensate). As a result, the measured engine load (rl_w) will be under-scaled which is key to keeping estimated manifold pressure (ps_w) slightly below actual pressure (pvdks_w) without making irrational changes to the VE model (KFURL) which converts pressure to load and load to pressure.

The KRKTE tab will help you calculate a value for KRKTE. Fill in the constants with the appropriate values and let it do the math.

<img src="/documentation/images/krkte.png" width="800">

### Injector Scaling (KRKTE / TVUB)

Swapping fuel injectors? Two things need updating in the BIN:

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

## Dual Injection (MED17 Only)

The 2.5T (and other MED17 dual-fuel platforms) run both port injection and direct injection simultaneously. This means two separate injector banks to characterize, two KRKTE scalars, two sets of dead times, and a fuel split ratio between them. Getting this wrong means one bank runs rich while the other runs lean, or worse, the ECU's fuel model diverges from reality and nothing downstream makes sense.

The Dual Injection tab has three sub-tabs:

### Port Injector Scaling

Calculate KRKTE_PFI (port injector constant) using the same math as ME7 KRKTE, but for the port injectors specifically. Enter the stock and new port injector flow rates, fuel pressures, and dead times. The calculator handles the Bernoulli pressure correction and TVUB scaling.

### Direct Injector Scaling

Calculate KRKTE_GDI for the direct injectors. Direct injectors run at significantly higher fuel pressure (200+ bar vs 4 bar for port), so the pressure correction factor matters more here.

### Split Calculator

Given a load point and RPM, calculate the fuel share between port and direct injectors. The 2.5T runs varying split ratios depending on the operating point — at idle it's mostly port injection, at WOT it shifts toward direct injection. If you've changed injector sizes on one bank but not the other, the split ratio needs recalculating.

<img src="/documentation/images/med17/dual_injection.png" width="800">

## MAF Scaling (MLHFM)

> **MED17 note:** MAF scaling (MLHFM) is an ME7-only workflow. MED17 does not use a static MAF linearization curve — it has an adaptive volumetric efficiency model (`fupsrl_w` / `fupsrls_w`) that self-corrects. The Closed Loop and Open Loop MAF scaling tabs are hidden when MED17 is selected.

With KRKTE dialed in, it's time to get your MAF telling the truth. This is where calibration starts to feel like actual tuning.

Read [MAF](https://s4wiki.com/wiki/Mass_air_flow)

In any MAFed application it may be necessary to increase the diameter of the MAF housing to extend the range of the sensor (while also reducing resolution) or to change MAF sensors entirely. Incorrect MAF linearization will lead to irrational changes in fueling (KFKHFM/FKKVS), the VE model (KFURL), and load request (LDRXN/KFMIRL). Having an accurate MAF makes tuning considerably simpler — or rather, having an *inaccurate* MAF makes everything else considerably harder.

To scale a MAF we need a source of truth to make corrections against — we can do that in two ways based on fueling. Since we know the size of the injectors, the injector duty cycle and the air-fuel ratio, actual airflow can be calculated and compared against the MAF to make corrections.

### Closed Loop

Uses narrowband O2 sensors and fuel trims (STFT/LTFT) to correct the MAF linearization at part-throttle and idle. This is an iterative process — log, correct, flash, repeat until fuel trims are near 0%. It's tedious. It works.

**What to log:** `nmot`, `fr_w`, `fra_w`, `uhfm_w`, `wdkba`, `B_lr`, `rl_w`

**How much data:** At least 75 minutes of mixed driving (highway, city, parking lot). More data = better corrections. Multiple log files can be loaded at once. Yes, 75 minutes is a lot of driving. Your MAF has 512 voltage bins — you need data in all of them.

<img src="/documentation/images/closed_loop_mlhfm.png" width="800">

For detailed step-by-step usage, algorithm description, and screenshots see the [Calibration Guide — Closed Loop](documentation/calibration-guide.md#closed-loop-mlhfm).

### Open Loop

Uses a wideband O2 sensor to correct the MAF linearization at WOT. Requires KRKTE and closed loop corrections to be completed first. You need a pre-cat wideband sensor — a tail sniffer likely isn't sufficient.

ME7Tuner matches WOT pulls between ME7Logger and Zeitronix logs by throttle position detection, then correlates data points by RPM within each matched pull. Both logs must contain the same number of pulls.

**What to log (ME7Logger):** `nmot`, `fr_w`, `fra_w`, `uhfm_w`, `mshfm_w`, `wdkba`, `B_lr`, `rl_w`, `lamsbg_w`, `ti_bl`

**What to log (Zeitronix):** AFR

<img src="/documentation/images/open_loop_mlhfm.png" width="800">

For detailed step-by-step usage, algorithm description, and screenshots see the [Calibration Guide — Open Loop](documentation/calibration-guide.md#open-loop-mlhfm).

## Pressure to Load (PLSOL)

PLSOL is a sanity check tool — the "does my plan even make sense?" calculator. It converts between pressure, load, airflow, and horsepower. The key insight is that the *only* parameter that affects load is pressure — barometric pressure, intake air temperature, and the pressure-to-load conversion (KFURL) are assumed constant.

This is where you find out if your hardware can actually support your ambitions. For example, 2.7L of displacement approaching 900hp requires ~3bar (45psi) relative / 4bar (60psi) absolute. If your turbo map says you can't make 4bar absolute, PLSOL just saved you a lot of time and money.

<img src="/documentation/images/plsol.png" width="800">

For detailed usage and screenshots see the [Calibration Guide — PLSOL](documentation/calibration-guide.md#plsol---pressure-to-load-conversion).

## Torque & Load Tables (KFMIOP / KFMIRL)

This is where the torque monitoring system lives, and getting it wrong means ME7 pulls power when you don't want it to.

> **MED17 naming:** These maps are called **KFLMIOP** and **KFLMIRL** in MED17. The math is identical — ME7Tuner handles the naming automatically when MED17 is selected.

### KFMIOP (Load/Fill to Torque)

KFMIOP describes optimal engine torque as a normalized value (0–100%) relative to the maximum load defined by the MAP sensor limit. When upgrading to a higher-pressure MAP sensor, KFMIOP must be rescaled so that the normalized torque requests remain rational for the new load range.

ME7Tuner analyzes the current KFMIOP to estimate the MAP sensor limit and real-world pressure limit, then generates a rescaled KFMIOP with new axes. The same axes are used for KFZWOP and KFZW so ignition timing scales correctly.

* Read [Torque Monitoring](https://s4wiki.com/wiki/Tuning#Torque_monitoring)

Empirical tuning points:
* Any part of KFMIOP (load/RPM range) that can only be reached above ~60% wped_w is unrestricted and can be raised to keep mimax high such that requested load does not get capped.
* Ensure that mibas remains below miszul to avoid intervention (which you will see in mizsolv) by lowering KFMIOP in areas reachable by actual measured load.

<img src="/documentation/images/kfmiop.png" width="800">

For detailed tuning philosophy, algorithm, and usage see the [Calibration Guide — KFMIOP](documentation/calibration-guide.md#kfmiop-loadfill-to-torque).

### KFMIRL (Torque to Load/Fill)

KFMIRL is the inverse of KFMIOP. It exists as a lookup optimization so the ECU doesn't have to search KFMIOP every time it converts a torque request into a load request. KFMIOP is the input and KFMIRL is the output — they must stay in sync or you'll get interventions that make no sense.

<img src="/documentation/images/kfmirl.png" width="800">

For detailed usage see the [Calibration Guide — KFMIRL](documentation/calibration-guide.md#kfmirl-torque-request-to-loadfill-request).

## Ignition Timing (KFZWOP / KFZW)

If you modified KFMIRL/KFMIOP, you need to modify KFZWOP (optimal ignition timing) and KFZW/2 (ignition timing) to reflect the new load range. ME7Tuner extrapolates the existing timing maps to the new x-axis range generated from KFMIOP.

*Pay attention to the output.* Extrapolation works well for linear functions, but usually doesn't for non-linear functions like ignition advance. Ignition timing is decidedly non-linear. Examine the output and make sure it is reasonable before using it — you will probably have to rework it. This is a starting point, not a finished product.

<img src="/documentation/images/kfzwop.png" width="800">

<img src="/documentation/images/kfzw.png" width="800">

For detailed algorithm and usage see the Calibration Guide for [KFZWOP](documentation/calibration-guide.md#kfzwop-optimal-ignition-timing) and [KFZW/2](documentation/calibration-guide.md#kfzw2-ignition-timing).

## Throttle Transition (KFVPDKSD)

In a turbocharged application the throttle is controlled by a combination of the turbo wastegate (N75 valve) and the throttle body valve. KFVPDKSD defines the pressure ratio at which the ECU transitions between throttle-controlled and boost-controlled operation — it's the handoff point between two completely different control strategies. ME7Tuner parses a directory of logs and determines at what RPM points a given boost level can be achieved.

**What to log:** `nmot`, `wdkba`, `pus_w`, `pvdks_w`

<img src="/documentation/images/kfvpdksd.png" width="800">

For detailed algorithm and usage see the [Calibration Guide — KFVPDKSD](documentation/calibration-guide.md#kfvpdksd-throttle-transition).

> **MED17 note:** KFVPDKSD is ME7-only. MED17 uses a different throttle control architecture that doesn't expose this calibration point. This tab is hidden when MED17 is selected.

## Throttle Body Choke Point (WDKUGDN)

Let's get something straight: **WDKUGDN is NOT an alpha-n calibration map.** We cannot stress this enough. We wrote an entire documentation file about it. We added warnings to the tool UI. We're putting it here in bold. And people will still adjust WDKUGDN to fix alpha-n. Please don't be that person.

WDKUGDN is a 1D Kennlinie (`RPM → throttle angle °`) that defines the **choked flow point** of the throttle body at each RPM — the angle at which airflow transitions from throttled to unthrottled. It defines a physical property of the throttle body. **Only change WDKUGDN if you physically change the throttle body diameter or engine displacement.**

Unless you have changed the throttle body or engine displacement, WDKUGDN should not have to be modified. If your alpha-n is off, see [Alpha-N Calibration](#alpha-n-calibration--diagnostic) for the maps you *actually* need to change.

<img src="/documentation/images/wdkugdn.png" width="800">

For the full choked flow algorithm, what WDKUGDN controls, and why changing it for alpha-n is wrong, see the [Calibration Guide — WDKUGDN](documentation/calibration-guide.md#wdkugdn-throttle-body-choke-point).

> **MED17 note:** WDKUGDN and the Alpha-N diagnostic are ME7-only tools. MED17 does not use the same throttle-model-based load estimation — the adaptive VE model handles this internally. These tabs are hidden when MED17 is selected.

## Alpha-N Calibration & Diagnostic

Alpha-N (speed-density) mode is what happens when the ECU runs without the MAF sensor. When the MAF is unplugged or fails (`B_ehfm = true`), the ECU switches from `mshfm_w` (MAF-measured airflow) to `msdk_w` (throttle-model-estimated airflow) as the sole load input. If `msdk_w` doesn't match reality, the car runs poorly — wrong fueling, wrong ignition timing, wrong boost targets. Basically, everything goes sideways.

The Alpha-N Diagnostic Tool compares `mshfm_w` against `msdk_w` to assess how well your car will run with the MAF unplugged and identifies exactly which maps need calibrating.

> **Important:** WDKUGDN defines the throttle body choke point. **Do NOT adjust WDKUGDN to fix alpha-n accuracy.** (Yes, we're saying it again. We will keep saying it.)

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

For detailed technical documentation including me7-raw.txt line references, see [`documentation/me7-alpha-n-calibration.md`](technical/me7/me7-alpha-n-calibration.md).

## Boost PID Linearization (LDRPID)

This one's a game-changer. LDRPID provides a feed-forward (pre-control) factor to the existing PID boost controller. Without it, the PID has to do all the work from scratch every time — hunting, overshooting, oscillating. With it, the PID starts from a known-good baseline and only has to make small corrections. The linearization process is a lot of work by hand, but ME7Tuner can parse millions of data points to produce the linearization table.

Read [Actual pre-control in LDRPID](http://nefariousmotorsports.com/forum/index.php?topic=12352.0title=)

**What to log:** `nmot`, `pvdks_w`, `pus_w`, `wdkba`, `ldtvm`, `gangi`

Get as many WOT pulls as possible across the full RPM range. Put all logs in a single directory and load them in ME7Tuner. The linearized duty cycle will be output in KFLDRL, and a new KFLDIMX with x-axis will be estimated from the linearized boost table.

<img src="/documentation/images/ldrpid.png" width="800">

**MED17 users:** The LDRPID workflow is identical, but the log signal names differ. Log the following with ScorpionEFI:

* `nmot_w` (RPM)
* `psrg_w` (Actual MAP — maps to `pvdks_w`)
* `pu_w` (Barometric — maps to `pus_w`)
* `wdkba` (Throttle Angle)
* `tvldste_w` (Final WGDC — maps to `ldtvm`)
* `gangi` (Gear)

ME7Tuner translates these automatically via the adapter layer.

For the full algorithm and step-by-step usage see the [Calibration Guide — LDRPID](documentation/calibration-guide.md#ldrpid-feed-forward-pid).

---

# Stage 3: Optimization

The Optimizer is where ME7Tuner goes from "useful calculator" to "how did we live without this."

It's a suggestion engine that analyzes WOT (Wide Open Throttle) logs and recommends corrections to the boost control and volumetric efficiency maps so that **actual pressure tracks pssol** (requested pressure) and **actual load tracks LDRXN** (maximum specified load).

The core philosophy is that ME7's internal physical model — converting between pressure and load via KFURL and KFPBRK — is mathematically sound. If the base maps are calibrated correctly, the ECU's requested values should match reality (barring mechanical limitations such as turbo overspooling, knock limiting, boost leaks, etc.). When there is a discrepancy, the Optimizer identifies exactly *where* the error is and suggests specific map changes to fix it. The model works — you just have to give it the right numbers.

For detailed documentation on how the Optimizer works internally, see [`documentation/optimizer-architecture.md`](technical/optimizer-architecture.md) and [`documentation/optimizer-algorithms.md`](technical/optimizer-algorithms.md).

**MED17 users:** The Optimizer works identically on MED17 — same three-phase algorithm, same iterative workflow. The differences are in the log format (ScorpionEFI instead of ME7Logger) and the signal names. MED17 does not have a static KFPBRK VE model — the adaptive `fupsrls_w` replaces it — so Phase 2 (VE Model) focuses on boost control accuracy rather than KFPBRK corrections.

## Prerequisites

Before using the Optimizer, you should have already:

1. **Calibrated KRKTE and TVUB** (Fueling tab) so the ECU knows the correct fuel injector constant and dead times
2. **Scaled MLHFM** (MAF sensor) using the Closed Loop and Open Loop tabs so that fuel trims are near 0% and actual AFR matches requested AFR
3. **Defined KFMIRL/KFMIOP** (load and torque tables) scaled for your MAP sensor limits
4. **Calibrated LDRPID** (feed-forward PID / KFLDRL) with at least a rough baseline

The Optimizer refines the boost control and VE model *after* these foundations are in place. Garbage in, garbage out — and the Optimizer will now tell you about the garbage.

**MED17 prerequisites are similar but simpler:**

1. **Calibrated KRKTE_PFI and KRKTE_GDI** (Dual Injection tab) for both injector banks
2. **Defined KFLMIOP/KFLMIRL** (torque and load tables) scaled for your hardware
3. **Rough KFLDRL baseline** from the LDRPID tool or manual tuning

MED17's adaptive VE model (`fupsrls_w`) eliminates the need for manual MAF scaling — one less thing to worry about.

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

#### MED17 Log Headers (ScorpionEFI)

| Parameter | ScorpionEFI Header | Maps To | Description |
|-----------|-------------------|---------|-------------|
| RPM | `nmot_w` | `nmot` | Engine speed |
| Throttle Plate Angle | `wdkba` | `wdkba` | Throttle position (degrees) |
| Wastegate Duty Cycle | `tvldste_w` | `ldtvm` | Final WGDC output (%) |
| Barometric Pressure | `pu_w` | `pus_w` | Ambient barometric pressure (mbar) |
| Absolute Pressure | `psrg_w` | `pvdks_w` | Actual absolute manifold pressure (mbar) |
| Requested Pressure | `pvds_w` | `pssol_w` | ECU's requested manifold pressure (mbar) |
| Requested Load | `rlsol_w` | `rlsol_w` | ECU's requested load (%) |
| Engine Load | `rl_w` | `rl_w` | Actual measured engine load (%) |

ME7Tuner maps these ScorpionEFI signals to the internal ME7 equivalents automatically. Configure the headers once in the Configuration tab and the Optimizer handles the translation.

## How It Works

The Optimizer operates in three phases. Each one builds on the last — don't skip ahead.

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

> **MED17 note:** MED17 does not use KFPBRK — it has an adaptive volumetric efficiency model (`fupsrls_w`). Phase 2 on MED17 still analyzes the load ratio but focuses on validating that the adaptive model is converging correctly. If the load ratio is persistently off, it may indicate a mechanical issue rather than a calibration problem.

### Phase 3: Intervention Check (Torque Limiters)

Sometimes LDRXN won't be reached because a torque monitor or intervention is secretly capping the request before it ever reaches the boost controller. These are the invisible walls that make you think your boost control is broken when it's actually working perfectly — it's just being told to target less than you think.

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

**MED17 (ScorpionEFI):** Log the following parameters during WOT pulls:

* `nmot_w` (RPM)
* `wdkba` (Throttle Plate Angle)
* `tvldste_w` (Final Wastegate Duty Cycle)
* `pu_w` (Barometric Pressure)
* `psrg_w` (Actual Absolute Manifold Pressure)
* `pvds_w` (Requested Pressure)
* `rlsol_w` (Requested Load)
* `rl_w` (Actual Engine Load)

Same rule: more WOT pulls across the full RPM range = better suggestions. ScorpionEFI logs work identically to ME7Logger logs — load a single file or a directory.

### Step 3: Load and Analyze

1. Go to the **Optimizer** tab
2. Adjust the parameters if needed:
   * **MAP Tolerance (mbar):** How close actual boost must be to requested boost for a data point to count as "on-target" (default: 30 mbar)
   * **LDRXN Target (%):** Your maximum specified load target for the intervention check (default: 191%)
   * **KFLDIMX Overhead (%):** How much headroom above KFLDRL to give the PID I-limiter (default: 8%)
   * **Min Throttle Angle:** Minimum throttle angle to qualify as WOT data (default: 80°)
3. Click **"Load ME7 Log File"** for a single log, or **"Load ME7 Log Directory"** to process multiple logs at once

> **MED17 users:** Click **"Load MED17 Log File"** or **"Load MED17 Log Directory"** instead. The Optimizer automatically detects the platform and uses the MED17 analyzer (`analyzeMed17`), which adapts the three-phase algorithm for MED17's signal names and adaptive VE model.

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

The Optimizer is designed for iterative use. Rome wasn't built in one WOT pull.

1. **First pass:** Fix boost control (Phase 1). Write the suggested KFLDRL and KFLDIMX, then take new logs.
2. **Second pass:** With boost on-target, fix the VE model (Phase 2). Write the suggested KFPBRK, then take new logs.
3. **Verify:** On the final pass, both pressure and load charts should show tight tracking between requested and actual values. Warnings should be clear.

If the maps are calibrated correctly, `pssol` should match `pvdks_w` and `rlsol` should match `rl_w` — the ECU's physical model just works. That's the whole point.

### Sensor Voltage Saturation Detection

All ME7 analog sensors output 0–5 V. When a tuned engine pushes a sensor beyond its measurement range, the voltage clips at or near 5 V and the ECU cannot measure the real value. The Optimizer detects this automatically — because garbage in, garbage out, and now the tool tells you about the garbage.

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

> **MED17 note:** Sensor saturation detection works on MED17 logs too. The MAP sensor auto-detection uses `psrg_w` (which maps to `pvdks_w` internally). MAF voltage saturation is less relevant on MED17 since the adaptive VE model reduces MAF dependency, but the Optimizer will still flag it if detected.

---

*ME7Tuner is free software. It comes with no warranty. If you send 25 psi into a motor that can handle 15 psi because you didn't read the output, that's between you and your engine builder.*

*Built with mass quantities of coffee by [TracQi Technology](https://github.com/TracqiTechnology).*
