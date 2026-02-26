# General

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

ME7Tuner is software that provides tools to help calibrate the MAF, primary fueling and torque/load requests. It is 
somewhat specific to an ME7 M-box ECU.

There are vast number of binary formats for ME7. TunerPro is equipped to handle most of them and supports MSB<->LSB,
and other complex definitions, but ME7Tuner is not. ME7Tuner is designed to work with a specific binary format,
specifically the B5 S4 2.7T M-box. If you have a different binary format, there is a possibility that ME7Tuner will not work.

I have no intention of modifying ME7Tuner to work with other binary formats, but you can certainly do so yourself.

See the example binary format and xdf in the `example` directory to see the supported binary format.

<img src="/documentation/images/me7Tuner.png" width="800">

# Warning

ME7Tuner is free software written by some guy on the internet. ***ME7Tuner comes with no warranty.*** Use at your own risk!

It is a certainty that ME7Tuner will produce garbage outputs at some point you will damage your engine if you do not know what you are doing.
ME7Tuner is software that *helps* you calibrate your engine. It does not calibrate your engine for you. It is not a replacement for knowledge of how to calibrate an engine.

## Installation

ME7Tuner is packaged as a JAR file. You will need to have Java installed on your system to run it. You can download Java from [here](https://www.oracle.com/java/technologies/downloads/).

Note that ME7Tuner built against Java 17, so you will need to have Java 17+ installed on your system to run it. Once you have Java 17+ installed and the JAR file, simply double click the JAR file to run it.

<a href="https://github.com/KalebKE/ME7Tuner/releases/latest">![GitHub Release](https://img.shields.io/github/v/release/KalebKE/ME7Tuner?color=GREEN)</a>

#### Table of contents
1. [Tuning Philosophy](#tuning-philosophy)
2. [Fueling (KRKTE & Injector Scaling)](#fueling-krkte--injector-scaling)
3. [MLHFM (MAF Scaling)](#mlhfm-maf-scaling)
4. [MLHFM - Closed Loop](#mlhfm---closed-loop)
5. [MLHFM - Open Loop](#mlhfm---open-loop)
7. [PLSOL (Pressure -> Load)](#plsol---rlsol-pressure-to-load-conversion)
8. [KFMIRL (Load)](#kfmirl-torque-request-to-loadfill-request)
9. [KFMIOP (Torque)](#kfmiop-loadfill-to-torque)
10. [KFZWOP (Optimal Ignition Timing)](#kfzwop-optimal-ignition-timing)
11. [KFZW/2 (Ignition Timing)](#kfzw2-ignition-timing)
13. [KFVPDKSD (Throttle Transition)](#kfvpdksd-throttle-transition)
14. [WDKUGDN (Throttle Body Choke Point)](#wdkugdn-throttle-body-choke-point)
15. [Alpha-N Calibration & Diagnostic Tool](#alpha-n-calibration--diagnostic-tool)
16. [LDRPID (Feed-Forward PID)](#ldrpid-feed-forward-pid)
17. [Optimizer (Pressure/Load Optimizer)](#optimizer-pressureload-optimizer)

# Tuning Philosophy

Everything in ME7 revolves around requested load (or cylinder fill).

* Read [Engine load](https://s4wiki.com/wiki/Load)

The simplified description of how ME7 works is as follows:

In ME7, the driver uses the accelerator pedal position to make a torque request. Pedal positions are mapped to a torque request (which is effectively a normalized load request). That torque request is then mapped to a load request. ME7 calculates how much pressure (boost) is required to achieve the load request which is highly dependent on hardware (the engine, turbo, etc...) and also the weather (cold, dry air is denser than hot, moist air). When tuning ME7 the goal is to calibrate the various maps to model the hardware and the weather accurately. If modeled incorrectly, ME7 will determine something is wrong and will protect the engine by reducing the capacity to produce power at various levels of intervention.

Note that no amount of modifications (intake, exhaust, turbo, boost controllers, etc...) will increase the power of the engine if actual load is already equal to or greater than requested load. ME7 will use interventions to *decrease* actual load (power) to get it equal requested load. You must calibrate the tune to request more load to increase power.

ME7Tuner can provide calculations that allow ME7 to be tuned with accurate airflow, pressure and load measurements which can simplify calibrations.

## READ ME FIRST! Do I need to use ME7Tuner?

You probably don't need to use ME7Tuner. For most applications, the stock M-box is sufficient to support the power you want to make.

In general ME7Tuner is only useful if you need to request more than 191% load on an M-Box. This means that K03 and most K04 configurations do not need the level of calibrations provided by ME7Tuner.

The following information should give you a good estimate of what hardware you need to achieve a given power goal, how much calibration you will need to do to support that power and if ME7Tuner is useful to you.

Refer to the following tables to determine if ME7Tuner is useful to you:

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

# Configuration

ME7Tuner works from a binary file and an XDF definition file. You will need to load these using the ME7Toolbar.

* File -> Open Bin
* XDF -> Select XDF

<img src="/documentation/images/xdf.png" width="800">

### Map Definitions

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

# Order of Calibrations

In general, you should start with a stock binary and follow order provided by this document. 

*It is critical that you calibrate primary fueling first*. 

Fueling is the ***one and only*** known constant to calibrate the MAF. It is highly recommended that you calibrate your fueling
with an accurate specification of the fuel injectors.

When the fueling has been calibrated, you can take logs to have ME7Tuner suggest a MAF scaling.

Once the fueling and MAF are calibrated load request, ignition advance and pressure (boost) requested can be calibrated.

# Fueling (KRKTE & Injector Scaling)

The Fueling tab consolidates all fuel-injector-related calibration into one place with two sub-tabs:

## KRKTE (Primary Fueling)

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

## Injector Scaling (KRKTE / TVUB)

When swapping fuel injectors (e.g., upgrading to larger injectors for more fueling headroom), two things need updating in the BIN:

### KRKTE — Injector Constant (Scalar)

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

### TVUB — Dead Time Table

TVUB is a 1D Kennlinie: battery voltage (V) → dead time (ms). Dead time (Ventilverzugszeit) is the electrical opening delay of the injector solenoid — it does NOT scale with flow rate. Each injector model has a unique dead time curve from the manufacturer's data sheet. If you only have the dead time at 14V, the calculator estimates the full curve using 1/V scaling.

### KFLF Clarification

KFLF exists in ME7 but is "Lambda map at partial load" — a partial-load AFR target map (RPM × load → lambda). It is **NOT** an injector linearization table. There is no "KFLFW" injector linearization in ME7. ME7 handles minimum pulse width via the TEMIN constant.

### Usage

1. Open the **Fueling** tab → **Injector Scaling** sub-tab
2. Enter the stock injector flow rate, fuel pressure, and dead time
3. Enter the new injector flow rate, fuel pressure, and dead time
4. Optionally provide a TVUB voltage table (voltage:deadtime pairs) from the injector data sheet
5. Click **Calculate** to get the KRKTE scale factor and TVUB table
6. Apply the results to your BIN

### Important Notes

- **Changing injectors does NOT require alpha-n (msdk_w) recalibration** — injectors affect fueling, not air measurement
- **Always update KRKTE** (via the KRKTE sub-tab) when changing injectors — KRKTE encodes the injector constant
- **Always reset ECU adaptations** after flashing new injector calibration, then drive with the MAF connected to re-learn fuel trim adaptations
- If the new injectors shift lambda enough to trigger O2 adaptation (KFKHFM), the MAF reading changes, which affects the msndko_w/fkmsdk_w learning — **verify alpha-n accuracy** with the Alpha-N Diagnostic (WDKUGDN tab) after re-learning

# MLHFM (MAF Scaling)

When you are satisfied with KRKTE, you will need to get your MAF scaled to the fuel injectors.

Read [MAF](https://s4wiki.com/wiki/Mass_air_flow)

In any MAFed application it may be necessary to increase the diameter of the MAF housing to extend the range of the sensor (while also reducing resolution) or to change MAF sensors entirely.

In general, a MAF sensor can be moved to a larger housing to extend the range of the sensor with a constant correction to the linearization curve (MLHFM) that defines the conversion between the MAF sensors voltage output to an estimation of airflow. This constant correction is usually based on the change in diameter from the current MAF housing to the new MAF housing.

If the MAF diameter can not be increased enough to achieve the desired range a new sensor (accompanied by a corresponding linearized curve) can be used to increase the range of the MAF housing.

###  Increasing MAF Diameter

Read [Diameter Effect on Airflow](https://s4wiki.com/wiki/Mass_air_flow#MAF_housing_diameter)

Significantly increasing the diameter of the MAF housing can change the airflow through the MAF housing enough that it
results in a *non-linear* change to the original linearization curve (MLHFM). Since the injector scaling (KRKTE) is fixed
(and linear) this means making changes in KFKHFM and/or FKKVS to get the fuel trims close to 0% corrections. This is 
difficult and tedious work. It is more simple to scale the MAF accurately and leave KFKHFM and FKKVS more or less alone.

### Changing MAF sensors

Changing to a MAF sensor with an increased range may be a better option than reusing your stock sensor in a larger diameter 
housing. Even if a transfer function is provided, you may find that the new sensor and housing in your specific configuration 
doesn't flow exactly as expected due to non-linearities in airflow at specific (or all) air velocities or other unknown 
irregularities. The original curve is inaccurate enough that KFKHFM and/or FKKVS would have to be significantly modified 
to get the engine to idle and WOT fueling safe. Again, it is more simple to scale the MAF accurately and leave KFKHFM and 
FKKVS more or less alone.

### Scaling Your MAF

Presumably, incorrect MAF linearization will lead to irrational changes in the following places at a minimum:

* Fueling -> KFKHFM/FKKVS/LAMFA/WDKUGDN
* VE model -> KFURL
* Load request -> LDRXN/KFMIRL

Having to make irrational changes in these places makes tuning considerably more difficult overall compared to just having an accurate MAF.

To scale a MAF we need a source of truth to make changes against we can do that in two ways based on fueling. Since we know the size of the injectors, the injector duty cycle and the air-fuel ratio... actual airflow can be calculated and compared against the MAF to make corrections.

* Closed loop fueling uses the narrowband O2 sensors and fuel trims to make corrections
* Open loop fueling uses a wideband 02 sensor to make corrections

# MLHFM - Closed Loop

This algorithm is roughly based on [mafscaling](https://github.com/vimsh/mafscaling/wiki/How-To-Use).

### Algorithm

The LTFT and STFT corrections at each voltage for MLHFM are calculated and then applied to the transformation.

The Correction Error at each measured voltage is calculated as **(STFT - 1) + (LTFT - 1)**, scaled by the ratio of the logged voltage to the nearest MLHFM voltage (**logged_voltage / nearest_MLHFM_voltage**). Voltages with fewer than 5 samples receive no correction.

The Total Correction is the average of the mean and mode of the Correction Errors at each measured voltage for MLHFM.

A 5-point moving average is applied to smooth the corrections before the optional polynomial fit.

The corrected kg/hr transformation for MLHFM is calculated as **current_kg/hr * (tot_corr + 1)**.

### Usage

The key is to get as much data as possible. Narrow band O2 sensors are noisy and slow, so the algorithm depends on lots of data and averaging to estimate corrections. The Closed Loop parser is designed to parse multiple log files at one time so you can compile logs over a period of time. The tune/hardware cannot change between logs. Also, it is advisable to log in consistent weather.

* Get [ME7Logger](http://nefariousmotorsports.com/forum/index.php/topic,837.0title,.html)

Log the following parameters:

* RPM - 'nmot'
* STFT - 'fr_w'
* LTFT - 'fra_w'
* MAF Voltage - 'uhfm_w'
* Throttle Plate Angle - 'wdkba'
* Lambda Control Active - 'B_lr'
* Engine Load - 'rl_w' (required by parser, not used in correction algorithm)

Logging Instructions:

* Log long periods of consistent throttle plate angles and boost. We are trying to capture data where the MAF's rate of change (delta) is as small as possible. You don't have to stop/start logging between peroids of being consistent since ME7Tuner will filter the data for you, but you still want as much of this data as possible.
* Stay out of open-loop fueling. We don't care about it (right now). Like inconsistent MAF deltas, ME7Tuner will filter out open-loop data.
* Get at least 30 minutes of driving on a highway. Vary gears and throttle positions often to get measurements at as many throttle angles and RPM combinations as possible. Finding a highway with a long, consistent incline is ideal since you can 'load' the engine resulting in higher MAF voltages without going into open-loop fueling. Remember to slowly roll on and off the throttle. Sudden changes will result in less usuable data.
* Get at least 30 minutes of typical 'city' driving. Stop lights, slower city speeds, lots of gears and throttle positions. Remember to be as consistent as possible rolling on and off of the throttle.
* Get at least 15 minutes of parking lot data. Drive slowly around the parking lot in 1st and 2nd gear. Stop and start often. Vary the throttle plate and RPM as much as possible.
* Save your log and put it into a directory (along with other closed-loop logs from the same tune if desired).
* Open ME7Tuner and click on the "Close Loop Fueling" tab at the top

<img src="/documentation/images/closed_loop_mlhfm.png" width="800">

* Click the "ME7 Logs" tab on the left side of the screen and click the "Load Logs" button at the bottom. Select the directory that contains your closed loop logs from ME7Logger. The derivative (dMAFv/dt) of the logged MAF voltages should plot on the screen. The vertical lines represent clusters of data at different derivative (rates of change, delta, etc...) for a given MAF voltage. You want to select the data under the smallest derivative possible while also including the largest voltage range as possible. I find 1 to be a good derivative to start with.
* Green samples are included by the filter. 
* Red samples are excluded by the filter.

<img src="/documentation/images/closed_loop_mlhfm_filter.png" width="800">

* Click "Configure Filter" in the bottom left corner of the screen. This is where you can configure the filter for the incoming data. You can filter data by a minimum throttle angle, a minimum RPM, a maximum derivative (1 is usually a good start).

* Click the "Correction" tab on the left side of the screen. You will see the current MLHFM plotted in blue and the corrected MLHFM plotted in red. The corrected MLHFM is also displayed in a table on the right hand side of the screen and can be copied directly into TunerPro. Clicking "Save MLFHM" will allow you to save MLFHM to a .csv file which can be loaded for the next set of corrections.

<img src="/documentation/images/closed_loop_mlhfm_corrected.png" width="800">

* Click the "dMAFv/dt" tab at the bottom of the screen. This displays the derivative of the filtered data used to calculate the corrections. Remember that a smaller derivative is better because the MAF's rate of change smaller (more stable).

<img src="/documentation/images/closed_loop_mlhfm_derivative.png" width="800">

* Click the "AFR Correction %" tab at the bottom of the screen. This displays the raw point cloud of Correction Errors with the Mean, Mode and Final AFR correction plotted on-top of the point cloud. Note how noisy the Correction Errors are.

<img src="/documentation/images/closed_loop_mlhfm_corrected_percentage.png" width="800">

* Load the corrected MLHFM into a tune, take another set of logs and repeat the process until you are satisfied with your STFT/LTFT at idle and part throttle.

* You may notice MLHFM starting to become 'bumpy' or 'not smooth' (for lack of a better term). This could be due to non-linearities in airflow due to changes in airflow velocity, but it is likely just noise we want to get rid of.  ME7Tuner has an option to fit your curve to a polynomial of a user configurable degree which will "smooth" your curve. Click the "Fit MLHFM" button with a reasonable polynomial degree (I find a 6th degree function to work well) to smooth your curve.

<img src="/documentation/images/closed_loop_mlhfm_corrected_best_fit.png" width="800">

# MLHFM - Open Loop

Before attempting to tune open loop fueling, you *need* to have KRKTE (fueling) and closed loop fueling nailed down. You also need a wideband O2 sensor that is pre-cat. A tail sniffer likely isn't sufficient here.

Note that ME7Tuner is designed to be used with Zeitronix logs, but logs from any wideband can be modified to use the expected headers.

Please open an issue with an example log file if you would like other formats to be supported.

### Algorithm

This algorithm is roughly based on [mafscaling](https://github.com/vimsh/mafscaling/wiki/How-To-Use).

The error from estimated airflow based on measured AFR + STFT + LTFT at each voltage for MLHFM are calculated and then applied to the transformation.

The raw AFR is calculated as wideband **AFR / ((100 - (LTFT + STFT)) / 100)**. 

The AFR % error is calculated as **(raw AFR - interpolated AFR) / interpolated AFR * 100)**, where interpolated AFR is interpolated from **(raw AFR - ECU Target AFR) / ECU Target AFR * 100)**.

The corrected kg/hr transformation for MLHFM is calculated as current_kg/hr * ((AFRerror% / 100) + 1).

### Usage

Unlike closed loop corrections, open loop logs must be contained a single ME7Logger file and a single Zeitronix log. Both ME7Logger and Zeitronix logger need to be started before the first pull and stopped after the last pull. ME7Tuner uses throttle position to detect WOT pull boundaries in each log, then matches pulls by order (1st ME7 pull with 1st Zeitronix pull, 2nd with 2nd, etc.). Within each matched pull, data points are correlated by RPM. Both sets of logs need to contain the same number of pulls.

* Get [ME7Logger](http://nefariousmotorsports.com/forum/index.php/topic,837.0title,.html)

Log the following parameters:

* RPM - 'nmot'
* STFT - 'fr_w'
* LTFT - 'fra_w'
* MAF Voltage - 'uhfm_w'
* MAF g/sec - 'mshfm_w'
* Throttle Plate Angle - 'wdkba'
* Lambda Control Active - 'B_lr'
* Engine Load - rl_w'
* Requested Lambda - 'lamsbg_w'
* Fuel Injector On-Time - 'ti_bl'

Logging Instructions:

* Start both ME7Logger and the Zeitronix Logger and do as many WOT pulls as possible. Perform WOT pulls in 2nd and 3rd gear from 2000 RPM if possible. Stop both loggers when you are finished.
* Save your logs and put them into a directory
* Open ME7Tuner and click on the "Open Loop Fueling" tab at the top

<img src="/documentation/images/open_loop_mlhfm.png" width="800">

* Click the "ME7 Logs" tab on the left side of the screen.
* Click "Load ME7 Logs" and select the ME7Logger .csv file
* Click "Load AFR Logs" and select the Zeitronix .csv file
* You should see the requested AFR from ME7 plotted in orange and the actual AFR from Zeitronix in red. *If the requested AFR doesn't match the actual AFR the MAF scaling is incorrect.*

<img src="/documentation/images/open_loop_mlhfm_logs.png" width="800">

* Click the "Airflow" tab at the bottom of the screen. You will see the airflow measured by the MAF in blue and the estimated airflow from the AFR in red. The measured airflow and estimated airflow should be the same or there the MAF scaling is incorrect.

<img src="/documentation/images/open_loop_mlhfm_airflow.png" width="800">

* Click the "Configure" filter button in the bottom left of the screen. You can see the minimum throttle angle, minimum RPM, minimum number of points from ME7Logger to count as a pull, the minimum number of points from Zeitronix to count as a pull and a maximum AFR. Note that Zeitronx can log at 40Hz while ME7Logger is usually 20Hz, so you may need to think about the number of points if your logging frequency is different.

* Click the "Correction" tab on the left side of the screen. You will see the current MLHFM plotted in blue and the corrected MLHFM plotted in red. The corrected MLHFM is also displayed in a table on the right side of the screen and can be copied directly into TunerPro. Clicking "Save MLFHM" will allow you to save MLFHM to a .csv file which can be loaded for the next set of corrections.

<img src="/documentation/images/open_loop_mlhfm_correction.png" width="800">

* Click the "AFR Correction %" tab at the bottom of the screen. This displays the raw point cloud of Correction Errors with the Mean, Mode and Final AFR correction plotted on-top of the point cloud. Note how noisy the Correction Errors are.

<img src="/documentation/images/open_loop_mlhfm_correction_percentage.png" width="800">

* Load the corrected MLHFM into a tune, take another set of logs and repeat the process until you are satisfied with your AFR at WOT.

* You may notice MLHFM starting to become 'bumpy' or 'not smooth' (for lack of a better term). This could be due to non-linearities in airflow due to changes in airflow velocity, but it is likely just noise we want to get rid of.  ME7Tuner has an option to fit your curve to a polynomial of a user configurable degree which will "smooth" your curve. Click the "Fit MLHFM" button with a reasonable polynomial degree (I find a 6th degree function to work well) to smooth your curve.

<img src="/documentation/images/open_loop_mlhfm_correction_best_fit.png" width="800">

# PLSOL -> RLSOL (Pressure to Load Conversion)

The pressure to load conversions are provided as a sanity check. The key is that the *only* parameter that affects load 
is pressure. The barometric pressure, intake air temperature and the pressure to load conversion (KFURL) are assumed to be constant.

In this simplified model the assumptions mean that as long as the turbocharger can produce the boost it will make as much power as any other turbocharger that can provide the boost. In other words, 16psi is 16psi no matter what produces that 16psi. Despite the simplicity
of this model I have found it to be very consistent with real world results.

You can edit the barometric pressure, the intake air temperature and pressure to load conversion to see how ME7 would respond to these parameters changing in the real world.

This estimates how much pressure is required to achieve a given load request. This is useful for determining if your hardware can support a given load request.

Note that for 2.7L of displacement to approach 900 horsepower the pressure required is approaching 3bar (45psi) relative and 4bar (60psi) absolute.

<img src="/documentation/images/plsol.png" width="800">

### PLSOL -> Airflow (Pressure to Airflow)

ME7Tuner will calculate the estimated airflow for a given load based on engine displacement (in liters) and RPM.

This estimates how much airflow is required to achieve a given load request. This is useful for determining if your MAF can support a given load request.

<img src="/documentation/images/plsol_airflow.png" width="800">

### PLSOL -> Power (Pressure to Horsepower)

ME7Tuner will calculate the estimated horsepower for a given load based on engine displacement (in liters) and RPM.

<img src="/documentation/images/plsol_power.png" width="800">

# KFMIOP (Load/Fill to Torque)

KFMIOP describes optimal engine torque.

Note that KFMIRL is the inverse of KFMIOP, not the other way around.

### Tuning Philosophy

KFMIOP is the optimum boost table for the engine's configuration, but it is expressed as a normalization relative to the maximum limit of the MAP sensor.

'Torque' is a normalized value between 0 and 1 (or 0% and 100%). KFMIOP represents a table of optimum torque values for the engine at a given load and RPM.

When we look at KFMIOP for a B5 S4 (M-box) we can see that the table is set up around a 2.5 bar (36psi) absolute MAP sensor limit
(the stock MAP sensor limit). A pressure of 2.5 bar run through the rlsol calculations to convert to a load request ends up being ~215% load. 

The maximum pressure that a K03 can efficiently produce is about 1bar (15psi) relative (2bar absolute) of pressure which results in ~191% load when run through the rlsol calculations.

<img src="/documentation/images/k03_compressor_map.JPG" width="500">

On the stock M-box KFMIOP you will note that the maximum load is limited to 191% and maximum torque is 89% which is because 191 is 89% of 215... or 191 load is 89% of 215 maximum load.
Each column in KFMIOP is mapping itself to a normalized peak torque value given by the peak load of 215% which is defined by the 2.5 bar MAP sensor.

KFMIOP can be converted to a boost table via the plsol calculation after you have derived peak load from the map. Looking at the boost table
it appears to be created empirically (on an engine dyno) and is tuned specifically for stock hardware (K03 turbos, etc...). So, unless you have access to an engine dyno there is no way to easily derive an OEM quality KFMIOP for your specific hardware configuration.

Despite this limitation, we can do better than simply extrapolating torque or rescaling the load axis. ME7Tuner takes a new
maximum MAP pressure, rescales the load axis and then rescales the torque based on the new maximum load. For example, if you go from
a max load of ~215% for a 2.5 MAP bar sensor to ~400% for 4 bar map sensor you would expect the torque request in the 9.75 load column
to be reduced by ~50% so the normalized torque isn't requesting a dramatically different value than 9.75. In other words, with
the 4 bar MAP sensor optimum torque at 9.75 has been reduced from ~4% to ~2% because the torque is normalized to the maximum load
dictated by the MAP sensor.

* Read [Torque Monitoring](https://s4wiki.com/wiki/Tuning#Torque_monitoring)

Additional empirical tuning points:

* Any part of KFMIOP (load/RPM range) that can only be reached above ~60% wped_w is unrestricted and can be raised to keep mimax high such that requested load does not get capped.
* Ensure that mibas remains below miszul to avoid intervention (which you will see in mizsolv) by lowering KFMIOP in areas reachable by actual measured load. This mainly becomes a concern in the middle of KFMIOP.

### Useage

* On the left side ME7Tuner will analyze the current KFMIOP and estimate the upper limit of the MAP sensor and the real world pressure limit (usually limited by the turbo) of the calibration.
* Based on the results of the analysis a boost table will be derived and can be viewed with the 'Boost' tab
* On the right side you can input the upper limit of the MAP sensor and your desired pressure limit
* After providing the MAP sensor limit and desired pressure limit ME7Tuner will output a new KFMIOP table and axis
* Copy the KFMIOP table and axis to other tables (KFMIRL/KFZWOP/KFZW) to generate corresponding maps.

<img src="/documentation/images/kfmiop.png" width="800">

Note that KFMIOP also produces axes for KFMIOP, KFZWOP and KFZW so you can scale your ignition timing correctly.

# KFMIRL (Torque request to Load/Fill request)

KFMIRL is the inverse of the KFMIOP map and exists entirely as an optimization so the ECU doesn't have to search KFMIOP every time it wants to covert a torque request into a load request.

<img src="/documentation/images/kfmirl.png" width="800">

### Algorithm

Inverts the input for the output.

### Usage

* KFMIOP is the input and KFMIRL is the output
* KFMIOP from the binary will be display by default
* Optionally modify KFMIOP to the desired values
* KFMIOP will be inverted to produce KFMIRL on the left side

# KFZWOP (Optimal Ignition Timing)

If you modified KFMIRL/KFMIOP you will want to modify the table and axis of KFZWOP to reflect to the new load range.

<img src="/documentation/images/kfzwop.png" width="800">

### Algorithm

The input KFZWOP is extrapolated to the input KFZWOP x-axis range (engine load from generated KFMIOP).

*Pay attention to the output!* Extrapolation can useful for linear functions, but usually isn't for non-linear functions (like optimal ignition advance). Examine the output and make sure it is reasonable before using it. You will probably have to rework the output.

### Useage

* Copy and paste your KFZWOP and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZWOP directly into TunerPro.

# KFZW/2 (Ignition Timing)

If you modified KFMIRL/KFMIOP you will want to modify the table and axis of KFZW/2 to reflect to the new load range.

<img src="/documentation/images/kfzw.png" width="800">

### Algorithm

The input KFZW/2 is extrapolated to the input KFZW/2 x-axis range (engine load from generated KFMIOP).

*Pay attention to the output!* Extrapolation can useful for linear functions, but usually isn't for non-linear functions (like optimal ignition advance). Examine the output and make sure it is reasonable before using it. You will probably have to rework the output.

### Useage

* Copy and paste your KFZW/2 and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZW/2 directly into TunerPro.

# KFVPDKSD (Throttle Transition)

In a turbocharged application the throttle is controlled by a combination of the turbo wastegate (N75 valve) and the throttle body valve. The ECU needs to know if the desired pressure can be reached at a given RPM.
If the pressure cannot be reached at the given RPM the throttle opens to 100% to minimize the restriction. If the pressure can be reached, the base throttle position is closed to the choke angle (defined in WDKUGDN), e.g the position at which it can start throttling.
The Z-axis of KFVPDKSD is a pressure ratio which is effectively the last 5% transitioning between atmospheric pressure (< 1) to boost pressure (> 1). In other words, the Z-axis is indicating where pressure will be greater than atmospheric (~1) or less than atmospheric (~0.95).
In areas where the desired pressure can be made, but that pressure is less than the wastegate cracking pressure (the N75 is unresponsive), the throttle is used to keep boost under control at the requested limit.

<img src="/documentation/images/kfvpdksd.png" width="800">

### Algorithm

ME7Tuner parses a directory of logs and determines at what RPM points a given boost level can be achieved.

### Useage

You will need a large set of logs.

Required Logged Parameters:

* RPM - 'nmot'
* Throttle plate angle - 'wdkba'
* Barometric Pressure - 'pus_w';
* Absolute Pressure - 'pvdks_w';

ME7Tuner:

* 'Load' a directory of logs using the 'Load Logs' button on the left side
* Wait for ME7Tuner to finishing the parse and calculations
* KFVPDKSD will be produced on the right side

# WDKUGDN (Throttle Body Choke Point)

WDKUGDN is a 1D Kennlinie (`RPM → throttle angle °`) that defines the **choked flow point** of the throttle body at each RPM. It tells the ECU at what throttle angle airflow transitions from **throttled** (restricted by the throttle plate) to **unthrottled** (no longer restricted — airflow is limited only by the turbo/engine).

> **⚠️ WDKUGDN is NOT an alpha-n calibration map.** It defines a physical property of the throttle body — the angle at which the throttle body stops restricting airflow. Adjusting WDKUGDN to "fix alpha-n" is a common misconception. **Only change WDKUGDN if you physically change the throttle body diameter or engine displacement.** See the [Alpha-N Calibration](#alpha-n-calibration--diagnostic-tool) section below for the correct maps to modify.

### What WDKUGDN Controls

When the throttle angle exceeds `WDKUGDN(rpm)`:
- The `B_ugds` flag is set (`true` = "unthrottled operation")
- The ECU stops using the throttle for load control and lets the turbo/wastegate control load instead
- Fuel adaptation (FUEREG) is disabled at WOT
- The throttle enters the "Überweg" (bypass) range — linear interpolation to 100%

When the throttle angle is below `WDKUGDN(rpm)`:
- The throttle is actively restricting airflow
- Throttle position is set via KFWDKMSN (mass flow → angle inverse map)
- The ECU uses the throttle angle as the primary load control actuator

### Why Changing WDKUGDN for Alpha-N is Wrong

WDKUGDN defines *when* the throttle chokes — it does **not** define *how much air flows* at a given angle. The common tuning advice ("compare msdk_w vs mshfm_w, adjust WDKUGDN") conflates two completely different problems:

| Problem | Correct Solution | **Wrong** Solution |
|---------|------------------|--------------------|
| msdk_w ≠ mshfm_w (alpha-n inaccuracy) | Calibrate BGSRM VE model (KFURL/KFPBRK/KFPRG), KFMSNWDK | Adjusting WDKUGDN |
| Throttle body physically changed | Recalculate WDKUGDN for new bore diameter | Adjusting KFMSNWDK |

Changing WDKUGDN when you should be changing KFMSNWDK or BGSRM maps will:
- Break the throttled/unthrottled transition point
- Incorrectly set/clear the `B_ugds` flag
- Disable fuel adaptation (FUEREG) at wrong throttle angles
- Cause the bypass ("Überweg") calculation to produce wrong values in the pressure ratio > 0.95 region

<img src="/documentation/images/wdkugdn.png" width="800">

### Algorithm

See https://en.wikipedia.org/wiki/Choked_flow.

The model assumes that a gas velocity will reach a maximum of the speed of sound at which point it is choked. 

Note that the mass flow rate can still be increased if the upstream pressure is increased as this increases the density of the gas entering the orifice, but the model assumes a fixed upstream pressure of 1013mbar (standard sea level).

Assuming ideal gas behaviour, steady-state choked flow occurs when the downstream pressure falls below a critical value which is '0.528 * upstreamPressure'. For example '0.528 * 1013mbar = 535mbar'. 535mbar on the intake manifold
side of the throttle body would cause the velocity of the air moving through the throttle body to reach the speed of sound and become choked.

The amount of air (or pressure assuming a constant density) an engine will consume for a given displacement and RPM can be calculated. How much air the throttle body will flow at a given throttle angle can be determined (KFWDKMSN/KFMSNWDK). 
Therefore, using the critical value of 0.528, the throttle angle at which choking occurs can be calculated to produce WDKUGDN.

While this model can used to achieve a baseline WDKUGDN, it appears that it has been tuned empirically. Unless you have changed the throttle body or engine displacement WDKUDGN should not have to be modified.

### Useage

* Calculate WDKUGDN for your engines displacement

# Alpha-N Calibration & Diagnostic Tool

Alpha-N (speed-density) mode is when the ECU runs without the MAF sensor. When the MAF is unplugged or fails (`B_ehfm = true`), the ECU switches from `mshfm_w` (MAF-measured airflow) to `msdk_w` (throttle-model-estimated airflow) as the sole load input. If `msdk_w` doesn't match reality, the car runs poorly — wrong fueling, wrong ignition timing, wrong boost targets.

The Alpha-N Diagnostic Tool compares `mshfm_w` against `msdk_w` to assess how well your car will run with the MAF unplugged and identifies exactly which maps need calibrating.

> **Important:** WDKUGDN defines the throttle body choke point — the angle at which airflow becomes unthrottled. **Do NOT adjust WDKUGDN to fix alpha-n accuracy.** WDKUGDN should only be changed if you have physically changed the throttle body diameter. See the [BGSRM VE Model](#bgsrm-ve-model--the-correct-maps-for-alpha-n) section below for the correct maps to modify.

## Background: Main vs Side Load Signals

ME7 uses two parallel load measurement paths:

| Path | Signal | Source | Role |
|------|--------|--------|------|
| **Main (Haupt)** | `mshfm_w` | HFM (MAF sensor) | Primary load signal — used when MAF is healthy |
| **Side (Neben)** | `msdk_w` | DK model (throttle + pressure model) | Backup load signal — used when MAF fails |

When the MAF is unplugged or fails (`B_ehfm = true`), the ECU switches entirely to `msdk_w`. If `msdk_w` doesn't match `mshfm_w`, the car will run poorly with wrong fueling, timing, and boost targets.

The ECU continuously adapts `msdk_w` to match `mshfm_w` via two learned values:
- **msndko_w** — additive offset compensating throttle body leak air
- **fkmsdk_w** — multiplicative factor compensating proportional errors

These adaptations **freeze when the MAF is unplugged** — they must be learned correctly beforehand.

## What Actually Needs Calibrating for Alpha-N

If `mshfm_w` and `msdk_w` diverge after hardware changes, here is what to calibrate (in priority order):

| Priority | Map/Parameter | When to Change |
|----------|--------------|----------------|
| 1 | **KFMSNWDK** (throttle body flow map) | Changed throttle body or intake manifold |
| 2 | **KFURL / KFPBRK / KFPRG** (VE model) | Changed cams, head work, or port work |
| 3 | **Adaptation reset** (msndko_w, fkmsdk_w) | After ANY map changes — let the ECU re-learn |
| 4 | **KUMSRL** (mass flow conversion) | Changed engine displacement |

**Fuel injector changes do NOT require alpha-n recalibration.** Injectors affect fueling, not air measurement. However, reset adaptations after injector changes to avoid stale learned values.

## BGSRM VE Model — The Correct Maps for Alpha-N

The BGSRM (Brennraum-Grundmodell Saugrohr-Modell — combustion chamber base model / intake manifold model) is the ME7 subsystem that converts between manifold pressure and relative load. **These are the maps that need calibrating when alpha-n is inaccurate** — not WDKUGDN.

The BGSRM formula:
```
rl = fupsrl_w × (ps - pirg_w) × FPBRKDS

where:
  fupsrl_w = KFURL(nmot) × fho_w × ftbr    → VE slope corrected for altitude + temp
  pirg_w   = KFPRG(nmot, wnw) × fho_w      → Residual gas partial pressure
  FPBRKDS  = KFPBRK(nmot, wnw)             → Volumetric efficiency correction factor
  ftbr     = f(tans, tmot, FWFTBRTA)        → Combustion chamber temperature correction
  fho_w    = pu / 1013.0                     → Altitude correction (barometric)
```

### The 6 Calibratable Components

| Component | What It Calibrates | In ME7Tuner? | Hardcoded? |
|-----------|-------------------|:------------:|:----------:|
| **KFURL** | VE slope (%/hPa per RPM) — how much load each hPa of pressure produces | ✅ KfurlSolver, `CalibrationSet.kfurlAt()` | No — read from BIN |
| **KFPBRK** | Combustion chamber correction (RPM × load → factor) — accounts for chamber shape, valve timing, flow losses | ✅ `suggestKfpbrkDelta()`, `CalibrationSet.kfpbrkAt()` | ⚠️ Plsol/Rlsol previously used constant `1.016`, now parameterized |
| **KFPRG** | Residual gas offset (hPa per RPM) — the manifold pressure at which cylinder filling = 0 | ✅ KfprgSolver, `CalibrationSet.kfprgAt()` | ⚠️ Previously hardcoded `70.0 hPa`, now parameterized |
| **FWFTBRTA** | IAT → ftbr weighting — how intake air temp blends with coolant temp for combustion chamber temp correction | ❌ Not read from BIN | ⚠️ Simplified formula (minor impact at WOT) |
| **PSMXN** | Max manifold pressure cap — physical limit on pressure in the VE model | ❌ Not in codebase | N/A |
| **KUMSRL** | Mass flow → load conversion constant — depends on displacement and cylinder count | ❌ Not read from BIN | Implicit in KFURL calculation |

### Which Component to Adjust Based on Error Pattern

| Symptom in Logs | Likely Cause | Map to Fix |
|----------------|-------------|------------|
| Load error proportional to pressure (grows linearly) | VE slope is wrong | **KFURL** |
| Constant load offset at each RPM (independent of pressure) | Residual gas offset is wrong | **KFPRG** |
| Non-linear load error (varies with both RPM and load) | VE correction factor is wrong at specific operating points | **KFPBRK** |
| Error varies with intake air temperature | Temperature weighting is off | **FWFTBRTA** |
| Error varies with altitude / barometric pressure | Barometric correction issue | Check baro sensor calibration |

> **For detailed technical documentation** of each component, including me7-raw.txt line references, calibration methods, and solver algorithms, see [`documentation/me7-alpha-n-calibration.md`](documentation/me7-alpha-n-calibration.md).

## Error Type Classification

The diagnostic tool classifies the dominant error between `mshfm_w` and `msdk_w`:

| Error Type | Pattern | Root Cause | Fix |
|-----------|---------|------------|-----|
| **ADDITIVE** | Constant offset regardless of airflow | msndko_w or MSLG wrong | Reset adaptations; check vacuum leaks |
| **MULTIPLICATIVE** | Error scales with airflow | fkmsdk_w or KFMSNWDK wrong | Reset adaptations; re-calibrate KFMSNWDK if at limits |
| **RPM_DEPENDENT** | Varies with RPM, not consistently with load | KFURL or KFPBRK wrong | Use Optimizer for per-RPM KFURL/KFPBRK correction |
| **MIXED** | Combination of above | Multiple issues | Reset adaptations first, then investigate per-RPM |

## Usage

### Step 1: Log with MAF Connected

Log these channels simultaneously across various RPM and load conditions (idle, cruise, part-throttle, WOT):

* `nmot` (RPM) — **required**
* `mshfm_w` (MAF-measured mass flow, kg/h) — **required**
* `msdk_w` (throttle-model-estimated mass flow, kg/h) — **required**
* `wdkba` (throttle angle) — optional but recommended
* `pvdks_w` (pre-throttle pressure, mbar) — optional, enables VE model solving
* `pus_w` (barometric pressure) — optional, enables VE model solving
* `rl_w` (relative load, %) — recommended for VE model accuracy

> **Note on pvdks_w:** This is the pressure **before** the throttle valve (compressor outlet), NOT manifold pressure. At WOT, pvdks ≈ manifold pressure (throttle fully open). The VE solvers automatically filter to WOT samples for accuracy.

### Step 2: Load and Analyze

1. Go to the **WDKUGDN** tab and select the **Alpha-N Diagnostic** sub-tab
2. Click **"Load Log File"** for a single log, or **"Load Log Directory"** for multiple logs
3. Review the results

### Step 3: Interpret Results

The diagnostic provides:
- **Severity rating:** GOOD (≤5%), WARNING (5–15%), or CRITICAL (>15%) average error
- **Error type classification:** Identifies whether the error is additive, multiplicative, or RPM-dependent
- **Per-RPM breakdown:** Shows error at each RPM bin to identify problem areas
- **Estimated corrections:** Multiplicative factor or additive offset to apply
- **Actionable recommendations:** Specific steps based on the error classification

# LDRPID (Feed-Forward PID)

Provide a feed-forward (pre-control) factor to the existing PID. Highly recommended. The linearization process can be a lot of work. ME7Tuner can do most of the work for you. You just need to provide the logs.

Read [Actual pre-control in LDRPID](http://nefariousmotorsports.com/forum/index.php?topic=12352.0title=)

<img src="/documentation/images/ldrpid.png" width="800">

### Algorithm

The algorithm is mostly based on [elRey's algorithm](http://nefariousmotorsports.com/forum/index.php?;topic=517.0). However, instead of using increments to build the linearization table, ME7Tuner uses a fit one-dimensional polynomial which can (and likely will) produce better results. ME7Tuner can also parse millions of data points to produce the linearization table versus the handful of points you would get from doing it by hand.

### Useage

* Log RPM (nmot), Actual Absolute Manifold Pressure (pvdks_w) and Barometric Pressure (pus_w), Throttle Plate Angle (wdkba), Wastegate Duty Cycle (ldtvm), and Selected Gear (gangi)

* Get as many WOT pulls starting from as low as an RPM as possible to as high as an RPM as possible. You will want a mix of "fixed" duty cycles and "real world" duty cycles.

* Put all of your logs in a single directory and load select the directory in ME7Tuner with "Load ME7 Logs"

* Wait awhile. It can take some time to parse the logs.

* The linearized duty cycle will be output in KFLDRL. Note that it may not be perfect and will likely take some additional massaging to get it right.

* For feed forward pre-control, ME7Tuner will output a new KFLDIMX and x-axis based on estimations from the linearized boost table. Keep in mind that this is just a ballpark estimation and will also likely require some massaging.

* I would advise requesting 95% duty cycle at any RPM ranges that can't produce the minimum boost required for cracking the wastegates (because you might as well be spooling as hard as you can here).

# Optimizer (Pressure/Load Optimizer)

The Optimizer is a suggestion engine that analyzes WOT (Wide Open Throttle) logs and recommends corrections to the boost control and volumetric efficiency maps so that **actual pressure tracks pssol** (requested pressure) and **actual load tracks LDRXN** (maximum specified load).

The core philosophy is that ME7's internal physical model — converting between pressure and load via KFURL and KFPBRK — is mathematically sound. If the base maps are calibrated correctly, the ECU's requested values should match reality (barring mechanical limitations such as turbo overspooling, knock limiting, boost leaks, etc.). When there is a discrepancy, the Optimizer identifies exactly *where* the error is and suggests specific map changes to fix it.

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
