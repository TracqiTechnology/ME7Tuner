# General

 <a href="https://github.com/KalebKE/ME7Tuner/releases/download/v1.0.1/ME7Tuner_v1.0.1.jar" rel="ME7Tuner_v1.0.1.jar">![ME7Tuner](https://img.shields.io/badge/ME7Tuner-v1.0-GREEN)</a>
 
ME7Tuner is software that provides tools to help calibrate the MAF, primary fueling and torque/load requests. It is somewhat specific to an ME7 M-box ECU.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.15.14-PM.png "ME7Tuner")

# Version 1.0

There are significant changes to ME7Tuner v1.0

* Updated from Java 8 to Java 17
* New Look and Feel
* Removed 3D rendering which causes lots of GPU problems on Linux systems
* The map definitions are user defined and can handle all versions of ME7 and potentially sub-sets of any ME version
* ME7Tuner now works from the binary to load tables
* Read and write directly to the binary
* ME7Tuner works from XDF files for map definitions
* KFKHFM has been removed
* KFWDKMSN inverse has been removed
* KFURL has been removed
* PLSOL -> RLSOL calculator has been added
* KFMIOP has been reworked to derive KFMIOP form user inputs
* KFMIRL is derived as the inverse of KFMIOP
* KFVPDKSD/E support has been added
* WDKUGDN is now derived from KFMSNWDK
* Improvements to LDRPID make the algorithm more stable

#### Table of contents
1. [Tuning Philosophy](#tuning-philosophy)
2. [KRKTE(Primary Fueling)](#krkte-primary-fueling)
3. [MLHFM (MAF Scaling)](#mlhfm-maf-scaling)
4. [MLHFM - Closed Loop](#mlhfm---closed-loop)
5. [MLHFM - Open Loop](#mlhfm---open-loop)
7. [PLSOL (Pressure -> Load)](#plsol---rlsol-pressure-to-load-conversion)
8. [KFMIRL (Load)](#kfmirl-torque-request-to-loadfill-request)
9. [KFMIOP (Torque)](#kfmiop-loadfill-to-torque)
10. [KFZWOP (Optimal Ignition Timing)](#kfzwop-optimal-ignition-timing)
11. [KFZW/2 (Ignition Timing)](#kfzw2-ignition-timing)
13. [KFVPDKSD (Throttle Transition)](#kfvpdksd-throttle-transition)
14. [WDKUGDN (Alpha-N Fueling)](#wdkugdn-alpha-n-fueling)
15. [LDRPID (Feed-Forward PID)](#ldrpid-feed-forward-pid)

# Tuning Philosophy

Everything in ME7 revolves around requested load (or cylinder fill).

* Read [Engine load](https://s4wiki.com/wiki/Load)

The simplified description of how ME7 works is as follows:

In ME7, the driver uses the accelerator pedal position to make a torque request. Pedal positions are mapped to a torque request (which is effectively a normalized load request). That torque request is then mapped to a load request. ME7 calculates how much pressure (boost) is required to achieve the load request which is highly dependent on hardware (the engine, turbo, etc...) and also the weather (cold, dry air is denser than hot, moist air). When tuning ME7 the goal is to calibrate the various maps to model the hardware and the weather accurately. If modeled incorrectly, ME7 will determine something is wrong and will protect the engine by reducing the capacity to produce power at various levels of intervention.

Note that no amount of modifications (intake, exhaust, turbo, boost controllers, etc...) will increase the power of the engine if actual load is already equal to or greater than requested load. ME7 will use interventions to *decrease* actual load (power) to get it equal requested load. You must calibrate the tune to request more load to increase power.

ME7Tuner can provide calculations that allow ME7 to be tuned with accurate airflow, pressure and load measurements which can simplify calibrations.

## Do I need to use ME7Tuner?

Not unless you have significantly increased power requirements. Refer to the following tables.

In general ME7Tuner is only useful if you need to request more than 191% load on an M-Box. This means that K03 and most K04 configurations do not need the level of calibrations provided by ME7Tuner.

##### Stock MAP Limit 

The stock MAP limit is the primary limitation to calibration. A stock M-box has just enough overhead to support K04's within their optimal efficiency range. 

* 2.5bar absolute (~22.45 psi relative)

Read [MAP Sensor](https://s4wiki.com/wiki/Manifold_air_pressure)  
Read [5120 Hack](http://nefariousmotorsports.com/forum/index.php?topic=3027.0)

##### Turbo Airflow

* K03 16 lbs/min (120 g/sec) (~160hp)
* K04 22 lbs/min (166 g/sec) (~225hp)
* RS6 25 lbs/min  (196 g/sec) (~265hp)
* 650R 37 lbs/min (287 g/sec) (~370hp)
* 770R 48 lbs/min (370 g/sec) ((~490hp)

Note: Remember to multiply by the number of turbos

##### MAF Airflow

* Stock Bosch/Hitachi 73mm (337 g/sec)
* Stock RS4 83mm (498 g/sec)
* Stock Hitachi 85mm (493 g/sec)
* HPX 89mm (800+ g/sec)

Read [MAF Sensor](https://s4wiki.com/wiki/Mass_air_flow)

##### Fuel for Airflow (10:1 AFR)

* K03 16 lbs/min air ->  ~1000 cc/min fuel
* K04 22 lbs/min air -> ~1400 cc/min fuel
* RS6 25 lbs/min air -> ~1600 cc/min fuel
* 650R 37 lbs/min air -> ~2200 cc/min fuel
* 770R 48 lbs/min air -> ~3024 cc/min fuel

Note: Remember to multiply air by the number of turbos and divide fuel by the number of fuel injectors

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

##### Summary

This information should give you a good estimate of what hardware you need to achieve a given power goal, how much calibration you will need to do to support that power and if ME7Tuner is useful to you.

# Configuration

ME7Tuner works from a binary file and an XDF definition file. You will need to load these using the ME7Toolbar.

* File -> Open Bin
* XDF -> Select XDF

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.10.20-PM.png "ME7Tuner Configuration")

### Map Definitions

You will need to tell ME7Tuner what definition you want to use for *all* fields. Pay attention to the units! ME7Tuner makes the following assumptions about units:

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

ME7Tuner automatically filters map definitions base on what is in the editable text box.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-06-at-10.29.15-AM.png "ME7Tuner Configuration")

### Log Headers

There are often many flavors of the same logged parameter. You can define the headers for the parameters that the log parser uses here.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-06-at-10.32.26-AM.png "ME7Tuner Configuration")

# Order of Calibrations

In general, you should start with a stock binary and follow order provided by this document. It is extremely important that you calibrate primary fueling *first*. Fueling is one known constant to calibrate the MAF which should be performed after fueling. Once the fueling and MAF are calibrated load request, ignition advance and pressure (boost) requested can be calibrated. Ignition advance and pressure request calibrations will need to be iterated upon as you approach your power goals.

# KRKTE (Primary Fueling)

* Read [Primary Fueling](https://s4wiki.com/wiki/Tuning#Primary) first

The first step is to calculate a reasonable value for KRKTE (primary fueling). This is the value that allows the ECU to determine how much fuel is required to achieve a given AFR (air fuel ratio) based on a requested load/cylinder filling. It is critical that KRKTE is close to the calculated value. If your KRKTE deviates significantly from the calculated value, your MAF is likely over/under scaled.

Pay attention to the density of gasoline (Gasoline Grams per Cubic Centimeter). The stock M-box assumes a value of 0.71 g/cc^3, but the [generally accepted density of gasoline](https://www.aqua-calc.com/page/density-table) is 0.75 g/cc^3. Also consider that ethanol has a density of 0.7893 g/cc^3 so high ethanol blends can be even denser.

Note that the decision to use a fuel density of 0.71 g/cc^3 (versus ~0.75 g/cc^3) will have the effect of under-scaling the MAF (more fuel will be injected per duty cycle so less airflow will need to be reported from the MAF to compensate). As a result, the measured engine load (rl_w) will be under-scaled which is key to keeping estimated manifold pressure (ps_w) slightly below actual pressure (pvdks_w) without making irrational changes to the VE model (KFURL) which converts pressure to load and load to pressure.

The KRKTE tab of ME7Tuner will help you calculate a value for KRKTE. Simply fill in the constants with the appropriate values.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.16.49-PM.png "Primary Fueling (KRKTE)")

# MLHFM (MAF Scaling)

When you are satisfied with KRKTE, you will need to get your MAF scaled to the fuel injectors.

Read [MAF](https://s4wiki.com/wiki/Mass_air_flow)

In any MAFed application it may be necessary to increase the diameter of the MAF housing to extend the range of the sensor (while also reducing resolution) or to change MAF sensors entirely.

In general, a MAF sensor can be moved to a larger housing to extend the range of the sensor with a constant correction to the linearization curve (MLHFM) that defines the conversion between the MAF sensors voltage output to an estimation of airflow. This constant correction is usually based on the change in diameter from the current MAF housing to the new MAF housing.

If the MAF diameter can not be increased enough to achieve the desired range a new sensor (accompanied by a corresponding linearized curve) can be used to increase the range of the MAF housing.

###  Increasing MAF Diameter

Read [Diameter Effect on Airflow](https://s4wiki.com/wiki/Mass_air_flow#MAF_housing_diameter)

Significantly increasing the diameter of the MAF housing can change the airflow through the MAF housing enough that it results in a *non-linear* change to the original linearization curve (MLHFM). Since the injector scaling (KRKTE) is fixed (and linear) this means making changes in KFKHFM and/or FKKVS to get the fuel trims close to 0% corrections. This is difficult and tedious work. It is more simple to scale the MAF accurately and leave KFKHFM and FKKVS more or less alone.

* **Solid Line** - 100mm housing scaled based on a constant derived from the change in housing diameter
* **Broken Line** - Estimated airflow based on fuel consumption and air-fuel ratio

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/100mmHitachi_vs_hpx.png "Under-scaled 100mm housing")

### Changing MAF sensors

Changing to a MAF sensor with an increased range may be a better option than reusing your stock sensor in a larger diameter housing. Even if a transfer function is provided, you may find that the new sensor and housing in your specific configuration doesn't flow exactly as expected due to non-linearities in airflow at specific (or all) air velocities or other unknown irregularities. The original curve is inaccurate enough that KFKHFM and/or FKKVS would have to be significantly modified to get the engine to idle and WOT fueling safe. Again, it is more simple to scale the MAF accurately and leave KFKHFM and FKKVS more or less alone.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/Original-and-Corrected-Curve.png "Changing MAF Sensors")

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

The average of your LTFT and STFT corrections at each voltage for MLHFM are calculated and then applied to the transformation.

The Correction Error is calculated as LTFT + STFT at each measured voltage for MLHFM.

The Total Correction is the average of the mean and mode of the Correction Errors at each measured voltage for MLHFM.

The corrected kg/hr transformation for MLHFM is calculated as **current_kg/hr * ((tot_corr% / 100) + 1)**.

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
* Engine Load - rl_w'

Logging Instructions:

* Log long periods of consistent throttle plate angles and boost. We are trying to capture data where the MAF's rate of change (delta) is as small as possible. You don't have to stop/start logging between peroids of being consistent since ME7Tuner will filter the data for you, but you still want as much of this data as possible.
* Stay out of open-loop fueling. We don't care about it (right now). Like inconsistent MAF deltas, ME7Tuner will filter out open-loop data.
* Get at least 30 minutes of driving on a highway. Vary gears and throttle positions often to get measurements at as many throttle angles and RPM combinations as possible. Finding a highway with a long, consistent incline is ideal since you can 'load' the engine resulting in higher MAF voltages without going into open-loop fueling. Remember to slowly roll on and off the throttle. Sudden changes will result in less usuable data.
* Get at least 30 minutes of typical 'city' driving. Stop lights, slower city speeds, lots of gears and throttle positions. Remember to be as consistent as possible rolling on and off of the throttle.
* Get at least 15 minutes of parking lot data. Drive slowly around the parking lot in 1st and 2nd gear. Stop and start often. Vary the throttle plate and RPM as much as possible.
* Save your log and put it into a directory (along with other closed-loop logs from the same tune if desired).
* Open ME7Tuner and click on the "Close Loop Fueling" tab at the top

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.18.54-PM.png "MLHFM")

* Click the "ME7 Logs" tab on the left side of the screen and click the "Load Logs" button at the bottom. Select the directory that contains your closed loop logs from ME7Logger. The derivative (dMAFv/dt) of the logged MAF voltages should plot on the screen. The vertical lines represent clusters of data at different derivative (rates of change, delta, etc...) for a given MAF voltage. You want to select the data under the smallest derivative possible while also including the largest voltage range as possible. I find 1 to be a good derivative to start with.
* Green samples are included by the filter. 
* Red samples are excluded by the filter.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.21.29-PM.png "Closed Loop Derivative")

* Click "Configure Filter" in the bottom left corner of the screen. This is where you can configure the filter for the incoming data. You can filter data by a minimum throttle angle, a minimum RPM, a maximum derivative (1 is usually a good start).

* Click the "Correction" tab on the left side of the screen. You will see the current MLHFM plotted in blue and the corrected MLHFM plotted in red. The corrected MLHFM is also displayed in a table on the right hand side of the screen and can be copied directly into TunerPro. Clicking "Save MLFHM" will allow you to save MLFHM to a .csv file which can be loaded for the next set of corrections.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.24.02-PM.png "Corrected closed loop MLHFM")

* Click the "dMAFv/dt" tab at the bottom of the screen. This displays the derivative of the filtered data used to calculate the corrections. Remember that a smaller derivative is better because the MAF's rate of change smaller (more stable).

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.25.07-PM.png "Filtered Closed Loop Std Dev")

* Click the "AFR Correction %" tab at the bottom of the screen. This displays the raw point cloud of Correction Errors with the Mean, Mode and Final AFR correction plotted on-top of the point cloud. Note how noisy the Correction Errors are.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.26.25-PM.png "Filtered Closed Loop AFR Corection%")

* Load the corrected MLHFM into a tune, take another set of logs and repeat the process until you are satisfied with your STFT/LTFT at idle and part throttle.

* You may notice MLHFM starting to become 'bumpy' or 'not smooth' (for lack of a better term). This could be due to non-linearities in airflow due to changes in airflow velocity, but it is likely just noise we want to get rid of.  ME7Tuner has an option to fit your curve to a polynomial of a user configurable degree which will "smooth" your curve. Click the "Fit MLHFM" button with a reasonable polynomial degree (I find a 6th degree function to work well) to smooth your curve.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-04-at-4.27.20-PM.png "Polynomial Fit MLHFM Corection%")

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

Unlike closed loop corrections, open loop logs must be contained a single ME7Logger file and a single Zeitronix log. Both ME7Logger and Zeitronix logger need to be started before the first pull and stopped after the last pull. ME7Tuner correlates the ME7Logger logs and Zeitronix logs based on throttle position so both sets of logs need to contain the same number of pulls.

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

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-11.01.06-AM.png "MLHFM")

* Click the "ME7 Logs" tab on the left side of the screen.
* Click "Load ME7 Logs" and select the ME7Logger .csv file
* Click "Load AFR Logs" and select the Zeitronix .csv file
* You should see the requested AFR from ME7 plotted in orange and the actual AFR from Zeitronix in red. *If the requested AFR doesn't match the actual AFR the MAF scaling is incorrect.*

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-11.04.58-AM.png "Open Loop Fueling")

* Click the "Airflow" tab at the bottom of the screen. You will see the airflow measured by the MAF in blue and the estimated airflow from the AFR in red. The measured airflow and estimated airflow should be the same or there the MAF scaling is incorrect.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-11.06.34-AM.png "Open Loop Airflow")

* Click the "Configure" filter button in the bottom left of the screen. You can see the minimum throttle angle, minimum RPM, minimum number of points from ME7Logger to count as a pull, the minimum number of points from Zeitronix to count as a pull and a maximum AFR. Note that Zeitronx can log at 40Hz while ME7Logger is usually 20Hz, so you may need to think about the number of points if your logging frequency is different.

* Click the "Correction" tab on the left side of the screen. You will see the current MLHFM plotted in blue and the corrected MLHFM plotted in red. The corrected MLHFM is also displayed in a table on the right side of the screen and can be copied directly into TunerPro. Clicking "Save MLFHM" will allow you to save MLFHM to a .csv file which can be loaded for the next set of corrections.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-11.07.35-AM.png "Open Loop MLHFM Correction")

* Click the "AFR Correction %" tab at the bottom of the screen. This displays the raw point cloud of Correction Errors with the Mean, Mode and Final AFR correction plotted on-top of the point cloud. Note how noisy the Correction Errors are.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-11.09.09-AM.png "Open Loop MLHFM AFR Correction%")

* Load the corrected MLHFM into a tune, take another set of logs and repeat the process until you are satisfied with your AFR at WOT.

* You may notice MLHFM starting to become 'bumpy' or 'not smooth' (for lack of a better term). This could be due to non-linearities in airflow due to changes in airflow velocity, but it is likely just noise we want to get rid of.  ME7Tuner has an option to fit your curve to a polynomial of a user configurable degree which will "smooth" your curve. Click the "Fit MLHFM" button with a reasonable polynomial degree (I find a 6th degree function to work well) to smooth your curve.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-11.10.38-AM.png "Open Loop Polynomial Fit")

# PLSOL -> RLSOL (Pressure to Load Conversion)

The pressure to load conversions are provided as a sanity check. The key is that the *only* parameter that affects load is pressure. The barometric pressure, intake air temperature and
the pressure to load conversion (KFURL) are assumed to be constant.

In this simplified model the assumptions mean that as long as the turbocharger can produce the boost it will make as much power as any other turbocharger that can provide the boost. In other words, 16psi is 16psi no matter what produces that 16psi. Despite the simplicity
of this model I have found it to be very consistent with real world results.

You can edit the barometric pressure, the intake air temperature and pressure to load conversion to see how ME7 would respond to these parameters changing in the real world.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-11.31.46-AM.png "PLSOL")

### PLSOL -> Airflow (Pressure to Airflow)

ME7Tuner will calculate the estimated airflow for a given load based on engine displacement (in liters) and RPM.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-3.24.06-PM.png "Airflow")

### PLSOL -> Power (Pressure to Horsepower)

ME7Tuner will calculate the estimated horsepower for a given load based on engine displacement (in liters) and RPM.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-3.25.31-PM.png "Horsepower")

# KFMIOP (Load/Fill to Torque)

KFMIOP describes optimal engine torque.

Note that KFMIRL is the inverse of KFMIOP, not the other way around.

### Algorithm

KFMIOP is the optimum boost table for the engine's configuration, but it is expressed as a normalization relative to the maximum limit of the MAP sensor.

'Torque' is a normalized value between 0 and 1 (or 0% and 100%). KFMIOP represents a table of optimum torque values for the engine at a given load and RPM.

When we look at KFMIOP for a B5 S4 (M-box) we can see that the table is set up around a 2.5 bar (36psi) absolute MAP sensor limit
(the stock MAP sensor limit). A pressure of 2.5 bar run through the rlsol calculations to convert to a load request ends up being ~215% load. 

The maximum pressure that a K03 can efficiently produce is about 1bar (15psi) relative (2bar absolute) of pressure which results in ~191% load when run through the rlsol calculations.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/K03s.webp "KO3 Flow")

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

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-11.13.30-AM.png "KFMIOP")

Note that KFMIOP also produces axes for KFMIOP, KFZWOP and KFZW so you can scale your ignition timing correctly.

# KFMIRL (Torque request to Load/Fill request)

KFMIRL is the inverse of the KFMIOP map and exists entirely as an optimization so the ECU doesn't have to search KFMIOP every time it wants to covert a torque request into a load request.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-1.23.58-PM.png "KFMIRL")

### Algorithm

Inverts the input for the output.

### Usage

* KFMIOP is the input and KFMIRL is the output
* KFMIOP from the binary will be display by default
* Optionally modify KFMIOP to the desired values
* KFMIOP will be inverted to produce KFMIRL on the left side

# KFZWOP (Optimal Ignition Timing)

If you modified KFMIRL/KFMIOP you will want to modify the table and axis of KFZWOP to reflect to the new load range.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-1.28.45-PM.png "KFZWOP")

### Algorithm

The input KFZWOP is extrapolated to the input KFZWOP x-axis range (engine load from generated KFMIOP).

*Pay attention to the output!* Extrapolation can useful for linear functions, but usually isn't for non-linear functions (like optimal ignition advance). Examine the output and make sure it is reasonable before using it. You will probably have to rework the output.

### Useage

* Copy and paste your KFZWOP and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZWOP directly into TunerPro.

# KFZW/2 (Ignition Timing)

If you modified KFMIRL/KFMIOP you will want to modify the table and axis of KFZW/2 to reflect to the new load range.

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-1.29.52-PM.png "KFZW")

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

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-1.50.59-PM.png "KFVPDKSD")

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

# WDKUGDN (Alpha-N Fueling)

WDKUGDN is the choked flow point of the throttle body at a given RPM. This value is critical for the ECU to estimate airflow based on a throttle angle. The ECU needs to know at what throttle angle airflow becomes throttled/unthrottled. 
For a given displacement, RPM and throttle body area there is a range beyond the choke point where increasing the throttle angle will not increase airflow. Inversely, there is a range where decreasing the throttle will not decrease airflow until the choke point has been reached. 
The transition between restricted airflow and unrestricted airflow is the choke point -> the point at which the throttle angle becomes either throttled or unthrottled. 

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-2.01.47-PM.png "WDKUGDN")

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

* Paste KFMSNWDK into the KFMSNWDK Table
* The inverse map will be output in the KFWDKMSN Table.

# LDRPID (Feed-Forward PID)

Provide a feed-forward (pre-control) factor to the existing PID. Highly recommended. The linearization process can be a lot of work. ME7Tuner can do most of the work for you. You just need to provide the logs.

Read [Actual pre-control in LDRPID](http://nefariousmotorsports.com/forum/index.php?topic=12352.0title=)

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

![alt text](http://kircherelectronics.com/wp-content/uploads/2022/03/Screen-Shot-2022-03-05-at-2.06.03-PM.png "LDRPID")
