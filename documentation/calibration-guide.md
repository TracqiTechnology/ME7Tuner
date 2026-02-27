# ME7Tuner Calibration Guide

This document contains detailed step-by-step usage instructions for each calibration tool in ME7Tuner. It is organized in calibration order — start at the top and work down.

For a high-level overview of the workflow and what each tool does, see the [README](../README.md#stage-2-calibration).

---

## Table of Contents

1. [Closed Loop MLHFM (MAF Scaling)](#closed-loop-mlhfm)
2. [Open Loop MLHFM (MAF Scaling)](#open-loop-mlhfm)
3. [PLSOL (Pressure to Load Conversion)](#plsol---pressure-to-load-conversion)
4. [KFMIOP (Load/Fill to Torque)](#kfmiop-loadfill-to-torque)
5. [KFMIRL (Torque to Load/Fill)](#kfmirl-torque-request-to-loadfill-request)
6. [KFZWOP (Optimal Ignition Timing)](#kfzwop-optimal-ignition-timing)
7. [KFZW/2 (Ignition Timing)](#kfzw2-ignition-timing)
8. [KFVPDKSD (Throttle Transition)](#kfvpdksd-throttle-transition)
9. [WDKUGDN (Throttle Body Choke Point)](#wdkugdn-throttle-body-choke-point)
10. [Alpha-N Calibration & Diagnostic Tool](#alpha-n-calibration--diagnostic-tool)
11. [LDRPID (Feed-Forward PID)](#ldrpid-feed-forward-pid)

---

## Closed Loop MLHFM

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

---

## Open Loop MLHFM

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

---

## PLSOL - Pressure to Load Conversion

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

---

## KFMIOP (Load/Fill to Torque)

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

### Usage

* On the left side ME7Tuner will analyze the current KFMIOP and estimate the upper limit of the MAP sensor and the real world pressure limit (usually limited by the turbo) of the calibration.
* Based on the results of the analysis a boost table will be derived and can be viewed with the 'Boost' tab
* On the right side you can input the upper limit of the MAP sensor and your desired pressure limit
* After providing the MAP sensor limit and desired pressure limit ME7Tuner will output a new KFMIOP table and axis
* Copy the KFMIOP table and axis to other tables (KFMIRL/KFZWOP/KFZW) to generate corresponding maps.

<img src="/documentation/images/kfmiop.png" width="800">

Note that KFMIOP also produces axes for KFMIOP, KFZWOP and KFZW so you can scale your ignition timing correctly.

---

## KFMIRL (Torque Request to Load/Fill Request)

KFMIRL is the inverse of the KFMIOP map and exists entirely as an optimization so the ECU doesn't have to search KFMIOP every time it wants to covert a torque request into a load request.

<img src="/documentation/images/kfmirl.png" width="800">

### Algorithm

Inverts the input for the output.

### Usage

* KFMIOP is the input and KFMIRL is the output
* KFMIOP from the binary will be display by default
* Optionally modify KFMIOP to the desired values
* KFMIOP will be inverted to produce KFMIRL on the left side

---

## KFZWOP (Optimal Ignition Timing)

If you modified KFMIRL/KFMIOP you will want to modify the table and axis of KFZWOP to reflect to the new load range.

<img src="/documentation/images/kfzwop.png" width="800">

### Algorithm

The input KFZWOP is extrapolated to the input KFZWOP x-axis range (engine load from generated KFMIOP).

*Pay attention to the output!* Extrapolation can useful for linear functions, but usually isn't for non-linear functions (like optimal ignition advance). Examine the output and make sure it is reasonable before using it. You will probably have to rework the output.

### Usage

* Copy and paste your KFZWOP and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZWOP directly into TunerPro.

---

## KFZW/2 (Ignition Timing)

If you modified KFMIRL/KFMIOP you will want to modify the table and axis of KFZW/2 to reflect to the new load range.

<img src="/documentation/images/kfzw.png" width="800">

### Algorithm

The input KFZW/2 is extrapolated to the input KFZW/2 x-axis range (engine load from generated KFMIOP).

*Pay attention to the output!* Extrapolation can useful for linear functions, but usually isn't for non-linear functions (like optimal ignition advance). Examine the output and make sure it is reasonable before using it. You will probably have to rework the output.

### Usage

* Copy and paste your KFZW/2 and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZW/2 directly into TunerPro.

---

## KFVPDKSD (Throttle Transition)

In a turbocharged application the throttle is controlled by a combination of the turbo wastegate (N75 valve) and the throttle body valve. The ECU needs to know if the desired pressure can be reached at a given RPM.
If the pressure cannot be reached at the given RPM the throttle opens to 100% to minimize the restriction. If the pressure can be reached, the base throttle position is closed to the choke angle (defined in WDKUGDN), e.g the position at which it can start throttling.
The Z-axis of KFVPDKSD is a pressure ratio which is effectively the last 5% transitioning between atmospheric pressure (< 1) to boost pressure (> 1). In other words, the Z-axis is indicating where pressure will be greater than atmospheric (~1) or less than atmospheric (~0.95).
In areas where the desired pressure can be made, but that pressure is less than the wastegate cracking pressure (the N75 is unresponsive), the throttle is used to keep boost under control at the requested limit.

<img src="/documentation/images/kfvpdksd.png" width="800">

### Algorithm

ME7Tuner parses a directory of logs and determines at what RPM points a given boost level can be achieved.

### Usage

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

---

## WDKUGDN (Throttle Body Choke Point)

WDKUGDN is a 1D Kennlinie (`RPM → throttle angle °`) that defines the **choked flow point** of the throttle body at each RPM. It tells the ECU at what throttle angle airflow transitions from **throttled** (restricted by the throttle plate) to **unthrottled** (no longer restricted — airflow is limited only by the turbo/engine).

> **Warning: WDKUGDN is NOT an alpha-n calibration map.** It defines a physical property of the throttle body — the angle at which the throttle body stops restricting airflow. Adjusting WDKUGDN to "fix alpha-n" is a common misconception. **Only change WDKUGDN if you physically change the throttle body diameter or engine displacement.** See the [Alpha-N Calibration](#alpha-n-calibration--diagnostic-tool) section below for the correct maps to modify.

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

### Usage

* Calculate WDKUGDN for your engines displacement

---

## Alpha-N Calibration & Diagnostic Tool

Alpha-N (speed-density) mode is when the ECU runs without the MAF sensor. When the MAF is unplugged or fails (`B_ehfm = true`), the ECU switches from `mshfm_w` (MAF-measured airflow) to `msdk_w` (throttle-model-estimated airflow) as the sole load input. If `msdk_w` doesn't match reality, the car runs poorly — wrong fueling, wrong ignition timing, wrong boost targets.

The Alpha-N Diagnostic Tool compares `mshfm_w` against `msdk_w` to assess how well your car will run with the MAF unplugged and identifies exactly which maps need calibrating.

> **Important:** WDKUGDN defines the throttle body choke point — the angle at which airflow becomes unthrottled. **Do NOT adjust WDKUGDN to fix alpha-n accuracy.** WDKUGDN should only be changed if you have physically changed the throttle body diameter. See the [BGSRM VE Model](#bgsrm-ve-model--the-correct-maps-for-alpha-n) section below for the correct maps to modify.

### Background: Main vs Side Load Signals

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

### What Actually Needs Calibrating for Alpha-N

If `mshfm_w` and `msdk_w` diverge after hardware changes, here is what to calibrate (in priority order):

| Priority | Map/Parameter | When to Change |
|----------|--------------|----------------|
| 1 | **KFMSNWDK** (throttle body flow map) | Changed throttle body or intake manifold |
| 2 | **KFURL / KFPBRK / KFPRG** (VE model) | Changed cams, head work, or port work |
| 3 | **Adaptation reset** (msndko_w, fkmsdk_w) | After ANY map changes — let the ECU re-learn |
| 4 | **KUMSRL** (mass flow conversion) | Changed engine displacement |

**Fuel injector changes do NOT require alpha-n recalibration.** Injectors affect fueling, not air measurement. However, reset adaptations after injector changes to avoid stale learned values.

### BGSRM VE Model — The Correct Maps for Alpha-N

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

#### The 6 Calibratable Components

| Component | What It Calibrates | In ME7Tuner? | Hardcoded? |
|-----------|-------------------|:------------:|:----------:|
| **KFURL** | VE slope (%/hPa per RPM) — how much load each hPa of pressure produces | Yes | No — read from BIN |
| **KFPBRK** | Combustion chamber correction (RPM × load → factor) — accounts for chamber shape, valve timing, flow losses | Yes | Previously used constant `1.016`, now parameterized |
| **KFPRG** | Residual gas offset (hPa per RPM) — the manifold pressure at which cylinder filling = 0 | Yes | Previously hardcoded `70.0 hPa`, now parameterized |
| **FWFTBRTA** | IAT → ftbr weighting — how intake air temp blends with coolant temp for combustion chamber temp correction | No | Simplified formula (minor impact at WOT) |
| **PSMXN** | Max manifold pressure cap — physical limit on pressure in the VE model | No | N/A |
| **KUMSRL** | Mass flow → load conversion constant — depends on displacement and cylinder count | No | Implicit in KFURL calculation |

#### Which Component to Adjust Based on Error Pattern

| Symptom in Logs | Likely Cause | Map to Fix |
|----------------|-------------|------------|
| Load error proportional to pressure (grows linearly) | VE slope is wrong | **KFURL** |
| Constant load offset at each RPM (independent of pressure) | Residual gas offset is wrong | **KFPRG** |
| Non-linear load error (varies with both RPM and load) | VE correction factor is wrong at specific operating points | **KFPBRK** |
| Error varies with intake air temperature | Temperature weighting is off | **FWFTBRTA** |
| Error varies with altitude / barometric pressure | Barometric correction issue | Check baro sensor calibration |

> **For detailed technical documentation** of each component, including me7-raw.txt line references, calibration methods, and solver algorithms, see [`documentation/me7-alpha-n-calibration.md`](me7-alpha-n-calibration.md).

### Error Type Classification

The diagnostic tool classifies the dominant error between `mshfm_w` and `msdk_w`:

| Error Type | Pattern | Root Cause | Fix |
|-----------|---------|------------|-----|
| **ADDITIVE** | Constant offset regardless of airflow | msndko_w or MSLG wrong | Reset adaptations; check vacuum leaks |
| **MULTIPLICATIVE** | Error scales with airflow | fkmsdk_w or KFMSNWDK wrong | Reset adaptations; re-calibrate KFMSNWDK if at limits |
| **RPM_DEPENDENT** | Varies with RPM, not consistently with load | KFURL or KFPBRK wrong | Use Optimizer for per-RPM KFURL/KFPBRK correction |
| **MIXED** | Combination of above | Multiple issues | Reset adaptations first, then investigate per-RPM |

### Usage

#### Step 1: Log with MAF Connected

Log these channels simultaneously across various RPM and load conditions (idle, cruise, part-throttle, WOT):

* `nmot` (RPM) — **required**
* `mshfm_w` (MAF-measured mass flow, kg/h) — **required**
* `msdk_w` (throttle-model-estimated mass flow, kg/h) — **required**
* `wdkba` (throttle angle) — optional but recommended
* `pvdks_w` (pre-throttle pressure, mbar) — optional, enables VE model solving
* `pus_w` (barometric pressure) — optional, enables VE model solving
* `rl_w` (relative load, %) — recommended for VE model accuracy

> **Note on pvdks_w:** This is the pressure **before** the throttle valve (compressor outlet), NOT manifold pressure. At WOT, pvdks ≈ manifold pressure (throttle fully open). The VE solvers automatically filter to WOT samples for accuracy.

#### Step 2: Load and Analyze

1. Go to the **WDKUGDN** tab and select the **Alpha-N Diagnostic** sub-tab
2. Click **"Load Log File"** for a single log, or **"Load Log Directory"** for multiple logs
3. Review the results

#### Step 3: Interpret Results

The diagnostic provides:
- **Severity rating:** GOOD (≤5%), WARNING (5–15%), or CRITICAL (>15%) average error
- **Error type classification:** Identifies whether the error is additive, multiplicative, or RPM-dependent
- **Per-RPM breakdown:** Shows error at each RPM bin to identify problem areas
- **Estimated corrections:** Multiplicative factor or additive offset to apply
- **Actionable recommendations:** Specific steps based on the error classification

---

## LDRPID (Feed-Forward PID)

Provide a feed-forward (pre-control) factor to the existing PID. Highly recommended. The linearization process can be a lot of work. ME7Tuner can do most of the work for you. You just need to provide the logs.

Read [Actual pre-control in LDRPID](http://nefariousmotorsports.com/forum/index.php?topic=12352.0title=)

<img src="/documentation/images/ldrpid.png" width="800">

### Algorithm

The algorithm is mostly based on [elRey's algorithm](http://nefariousmotorsports.com/forum/index.php?;topic=517.0). However, instead of using increments to build the linearization table, ME7Tuner uses a fit one-dimensional polynomial which can (and likely will) produce better results. ME7Tuner can also parse millions of data points to produce the linearization table versus the handful of points you would get from doing it by hand.

### Usage

* Log RPM (nmot), Actual Absolute Manifold Pressure (pvdks_w) and Barometric Pressure (pus_w), Throttle Plate Angle (wdkba), Wastegate Duty Cycle (ldtvm), and Selected Gear (gangi)

* Get as many WOT pulls starting from as low as an RPM as possible to as high as an RPM as possible. You will want a mix of "fixed" duty cycles and "real world" duty cycles.

* Put all of your logs in a single directory and load select the directory in ME7Tuner with "Load ME7 Logs"

* Wait awhile. It can take some time to parse the logs.

* The linearized duty cycle will be output in KFLDRL. Note that it may not be perfect and will likely take some additional massaging to get it right.

* For feed forward pre-control, ME7Tuner will output a new KFLDIMX and x-axis based on estimations from the linearized boost table. Keep in mind that this is just a ballpark estimation and will also likely require some massaging.

* I would advise requesting 95% duty cycle at any RPM ranges that can't produce the minimum boost required for cracking the wastegates (because you might as well be spooling as hard as you can here).
