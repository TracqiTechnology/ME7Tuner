# General

 <a href="https://github.com/KalebKE/ME7Tuner/releases/download/v0.2/ME7Tuner_v0.2.jar" rel="ME7Tuner.jar">![ME7Tuner](https://img.shields.io/badge/ME7Tuner-v0.2-GREEN)</a>
 
ME7Tuner is software that provides tools to help calibrate the MAF, primary fueling and torque/load requests. It is somewhat specific to an ME7 M-box ECU.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/LDRPID.png "ME7Tuner")

#### Table of contents
1. [Tuning Philosophy](#tuning-philosophy)
2. [KRKTE(Primary Fueling)](#krkte-primary-fueling)
3. [MLHFM (MAF Scaling)](#mlhfm-maf-scaling)
4. [MLHFM - Closed Loop](#mlhfm---closed-loop)
5. [KFKHFM - Closed Loop](#kfkhfm---closed-loop)
6. [MLHFM - Open Loop](#mlhfm---open-loop)
7. [MLHFM - Open Loop](#mlhfm---open-loop)
8. [KFMIRL (Load)](#kfmirl-torque-request-to-loadfill-request)
9. [KFMIOP (Torque)](#kfmiop-loadfill-to-torque)
10. [KFZWOP (Optimal Ignition Timing)](#kfzwop-optimal-ignition-timing)
11. [KFZW/2 (Ignition Timing)](#kfzw2-ignition-timing)
12. [KFURL (VE)](#kfurl-ve)
13. [WDKUGDN (Alpha-N Fueling)](#wdkugdn-alpha-n-fueling)
14. [KFWDKMSN (Alpha-N Fueling)](#kfwdkmsn-alpha-n-fueling)
15. [LDRPID (Feed-Forward PID)](#ldrpid-feed-forward-pid)

# Tuning Philosophy

Everything in ME7 revolves around requested load (or cylinder fill).

* Read [Engine load](https://s4wiki.com/wiki/Load)

The deeply simplified description of how ME7 works is as follows:

In ME7, the driver uses the accelerator pedal position to make a torque request. Pedal positions are mapped to a torque request (which is effectively a normalized load request). That torque request is then mapped to a load request. ME7 then calculates how much pressure (boost) is required to achieve the load request which is highly dependent on hardware (the engine, turbo, etc...) and also the weather (cold, dry air is denser than hot, moist air). When you are tuning ME7, you are trying to modify the various maps to model your hardware and the weather accurately. If you don't model things correctly, ME7 is going to decide something is wrong and will try to protect the engine by reducing the capacity to produce power at various levels of intervention. It is important to get things right.

Note that no amount of modifications (intake, exhaust, turbo, boost controllers, etc...) will fundamentally increase the power of the engine if actual load is already equal to or greater than requested load. ME7 will simply use interventions to decrease actual load to get it to equal or below requested load. You must modify the tune to request more load to significantly increase power.

ME7Tuner can provide calculations that allow ME7 to be tuned with accurate airflow and load measurements (a properly scaled MAF) which can make tuning the car much easier.

## Do I need to use ME7Tuner?

If have increased your changed your MAF sensor and/or MAF housing diameter and can make significantly more than 191% load, ME7Tuner can be helpful.

##### Stock MAP Limit 

* 2.5bar absolute (~22.45 psi relative)

Read [MAP Sensor](https://s4wiki.com/wiki/Manifold_air_pressure)
Read [5120 Hack](http://nefariousmotorsports.com/forum/index.php?topic=3027.0titl

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

Note that a stock M-box has a maximum load request of 191%.

##### Summary

This information should give you a good estimate of what hardware you need to acheive a given power goal, how much tuning you will need to do to support that power and if ME7Tuner is useful to you.

# KRKTE (Primary Fueling)

* Read [Primary Fueling](https://s4wiki.com/wiki/Tuning#Primary) first

The first step is to calculate a reasonable value for KRKTE (primary fueling). The is the value that allows the ECU to determine how much fuel is required to achieve a given AFR (air fuel ratio) based on a requested load/cyclinder filling. It is critial that KRKTE is close to the calculated value. If your KRKTE deviates significantly from the calculated value, your MAF is likely over/under scaled.

Pay attention to the density of gasoline (Gasoline Grams per Cubic Centimeter). The stock M-box assumes a value of 0.71 g/cc^3, but the [generally accepted density of gasoline](https://www.aqua-calc.com/page/density-table) is 0.75 g/cc^3. Also consider that ethanol has a density of 0.7893 g/cc^3 so high ethanol blends can be even denser.

Note that the decision to use a fuel density of 0.71 g/cc^3 (versus a more accurate ~0.75 g/cc^3) was clearly intentional and will have the effect of slightly underscaling the MAF (more fuel will be injected per duty cycle so less airflow will need to be reported from the MAF to compenstate). As a result, the measured engine load (rl_w) will be underscaled which is key to keeping estimated manifold pressure (ps_w) slightly below actual pressure (pvdks_w) without making irrational changes to the VE model (KFURL) which converts pressure to load and load to pressure.

The KRKTE tab of ME7Tuner will help you calculate a value for KRKTE. Simply fill in the constants with the appropriate values.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-1.36.38-PM.png "Primary Fueling (KRKTE)")

When you are satisfied with KRKTE, you will need to get your MAF scaled to the fuel injectors.

# MLHFM (MAF Scaling)

Read [MAF](https://s4wiki.com/wiki/Mass_air_flow)

In any MAFed application it may be necessary to increase the diameter of the MAF housing to extend the range of the sensor (while also reducing resolution) or to change MAF sensors entirely.

In general, a MAF sensor can be moved to a larger housing to extend the range of the sensor with a constant correction to the linearization curve (MLHFM) that defines the conversion between the MAF sensors voltage output to an estimation of airflow. This constant correction is usually based on the change in diameter from the current MAF housing to the new MAF housing.

If the MAF diameter can not be increased enough to acheieve the desired range a new sensor (hopefully accompanied with a corresponding linearation curve) can be used to increase the range of the MAF housing.

###  Increasing MAF Diameter

Read [Diameter Effect on Airflow](https://s4wiki.com/wiki/Mass_air_flow#MAF_housing_diameter)

* **Solid Line** - 100mm housing scaled based on a constand derived from the change in housing diameter
* **Broken Line** - Estimated airflow based on fuel consumption and air-fuel ratio

Significantly increasing the diamater of the MAF housing can change the airflow through the MAF housing enough that it results in a *non-linear* change to the original linearization curve (MLHFM). Since the injector scaling (KRKTE) is fixed (and linear) this means making changes in KFKHFM and/or FKKVS to get the fuel trims close to 0% corrections. This is difficult and tedious work. It is much easier to scale the MAF accurately and leave KFKHFM and FKKVS more or less alone.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/100mmHitachi_vs_hpx.png "Underscaled 100mm housing")

### Changing MAF sensors

Changing to a MAF sensor with an increased range may be a better option than reusing your stock sensor in a larger diameter housing. Even if a transfer function is provided, you may find that the new sensor and housing in your specific configuration doesn't flow exactly as expected due to non-linearities in airflow at specific (or all) air velocities or other unknown irregularities. The original curve is inaccurate enough that KFKHFM and/or FKKVS would have to be significantly modified to get the engine to idle and WOT fueling safe. Again, it is much easier to scale the MAF accurately and leave KFKHFM and FKKVS more or less alone.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/Original-and-Corrected-Curve.png "Changing MAF Sensors")

### Scaling Your MAF

Presumably, incorrect MAF linearization will lead to irrational changes in the following places at a minimum:

* Fueling -> KFKHFM/FKKVS/LAMFA/WDKUGDN
* VE model -> KFURL
* Load request -> LDRXN/KFMIRL

Having to make irrational changes in these places makes tuning considerably more difficult overall compared to just having an accurate MAF.

To scale a MAF we need a source of truth to make changes against we we can do that in two ways based on fueling. Since we know the size of the injectors, the injector duty cycle and the air-fuel ratio... actual airflow can be calculated and compared against the MAF to make corrections.

* Closed loop fueling uses the narrow band O2 sensors and fuel trims to make corrections
* Open loop fueling uses a wide-band 02 sensor to make corrections

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
* Log RPM (nmot), STFT (fr_w), LTFT (fra_w), MAF Voltage (uhfm_w), Throttle Plate Angle (wdkba) and Lambda Control Active (B_lr).
* Log long periods of consistent throttle plate angles and boost. We are trying to capture data where the MAF's rate of change (delta) is as small as possible. You don't have to stop/start logging between peroids of being consistent since ME7Tuner will filter the data for you, but you still want as much of this data as possible.
* Stay out of open-loop fueling. We don't care about it (right now). Like inconsistent MAF deltas, ME7Tuner will filter out open-loop data.
* Get at least 30 minutes of driving on a highway. Vary gears and throttle positions often to get measurements at as many throttle angles and RPM combinations as possible. Finding a highway with a long, consistent incline is ideal since you can 'load' the engine resulting in higher MAF voltages without going into open-loop fueling. Remember to slowly roll on and off the throttle. Sudden changes will result in less usuable data.
* Get at least 30 minutes of typical 'city' driving. Stop lights, slower city speeds, lots of gears and throttle positions. Remember to be as consistent as possible rolling on and off of the throttle.
* Get at least 15 minutes of parking lot data. Drive slowly around the parking lot in 1st and 2nd gear. Stop and start often. Vary the throttle plate and RPM as much as possible.
* Save your log and put it into a directory (along with other closed-loop logs from the same tune if desired).
* If you haven't done so already, create a .csv file of your MLHFM with headers of "voltage" and "kg/hr" and the corresponding values under each header. [Example mlhfm.csv](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/mlhfm.csv)
* Open ME7Tuner and click on the "Close Loop Fueling" tab at the top
* Click the MLHFM tab on the left and click the "Load MLHFM" button and select your mlhfm.csv file. The file should load and plot.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-2.52.22-PM.png "MLHFM")

* Click the "ME7 Logs" tab on the left hand side of the screen and click the "Load Logs" button at the bottom. Select the directory that contains your closed loop logs from ME7Logger. The derivative (dMAFv/dt) of the logged MAF voltages should plot on the screen. The vertical lines represent clusters of data at different derivative (rates of change, delta, etc...) for a given MAF voltage. You want to select the data under the smallest derivative possible while also including the largest voltage range as possible. I find 1 to be a good derivative to start with.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-2.59.31-PM.png "Closed Loop Std Dev")

* Click "Configure Filter" in the bottom left hand corner of the screen. This is where you can configure the filter for the incoming data. You can filter data by a minimum throttle angle, a minimum RPM, a maximum derivative (as discussed 1 is usually a good start).

* Click the "Correction" tab on the left side of the screen. You will see the current MLHFM plotted in blue and the corrected MLHFM plotted in red. The corrected MLHFM is also displayed in a table on the right hand side of the screen and can be copied directly into TunerPro. Clicking "Save MLFHM" will allow you to save MLFHM to a .csv file which can be loaded for the next set of corrections.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-3.08.05-PM.png "Corrected closed loop MLHFM")

* Click the "Std Dev" tab at the bottom of the screen. This displays the derivative of the filtered data used to calcuate the corrections. Remember that a smaller derivative is better because the MAF's rate of change smaller (more stable).

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-3.13.26-PM.png "Filtered Closed Loop Std Dev")

* Click the "AFR Correction %" tab at the bottom of the screen. This displays the raw point cloud of Correction Errors with the Mean, Mode and Final AFR correction plotted ontop of the point cloud. Note how noisy the Correction Errors are.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-4.08.32-PM.png "Filtered Closed Loop AFR Corection%")

* Load the corrected MLHFM into a tune, take another set of logs and repeat the process until you are satisfied with your STFT/LTFT at idle and part throttle.

* You may notice MLHFM starting to become 'bumpy' or 'not smooth' (for lack of a better term). This could be due to non-linearities in airflow due to changes in airflow velocity, but it is likely just noise we want to get rid of.  ME7Tuner has an option to fit your curve to a polynomial of a user configurable degree which will "smooth" your curve. Click the "Fit MLHFM" button with a reasonable polynomial degree (I find a 6th degree function to work well) to smooth your curve.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/MLHFM_POLYNOMIAL_FIT.png "Polynomial Fit MLHFM Corection%")

# KFKHFM - Closed Loop 

After performing a few Closed Loop MLHFM corrections, you may notice MLHFM starting to become 'bumpy' or 'not smooth' (for lack of a better term). This can be due to non-linearities in the intake tract where, for a given engine load, the velocity of the intake air is causing the MAF to read different values. If smoothing MLHFM can't get the 'bumpiness' to go away you can use KFKHFM to compensate for these non-linearities at specific loads and RPMs.

* Note that reducing the MAF housing diameter can help with reducing non-linearities. If you can't reduce the MAF housing diameter, changing MAF sensors to a HPX slot sensor can be a good option, too. If all else fails, then I would change KFKHFM as a last resort.

* Also note that FKKVS exists to fix fuel system non-linearities which can also result in unexpected MLHFM calibrations. However, there is no reason not to run very linear injectors like EV14's and not bother with FKKVS at all.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/08/mlhfm_lumpy.png "KFKHFM Close Loop Samples")

This algorithm is roughly based on [mafscaling](https://github.com/vimsh/mafscaling/wiki/How-To-Use).

### Algorithm

The algorithm for KFKHFM is exactly the same as MLHFM but the correction is applied the unit-less scalar values of KFKHFM instead of the kg/hr values of MLHFM. Notably, the KFKHFM correction is more complex in that it is applied to a three-dimensional map as opposed to the two-dimensional MLHFM map.

The average of your LTFT and STFT corrections at each load and RPM point for KFKHFM are calculated and then applied to the transformation.

The Correction Error is calculated as LTFT + STFT at each measured load and RPM point for KFKHFM.

The Total Correction is the average of the mean and mode of the Correction Errors at each measured load and RPM point for KFKHFM.

The corrected unit-less scalar transformation for KFKHFM is calculated as **scalar_value/hr * ((tot_corr% / 100) + 1)**.

### Usage

If your STFT and LTFT are more or less dialed in and you have a 'bumpy' MLFHM you can use KFKHFM to 'smooth' MLHFM and compenstate for the non-linearities on the intake tract.

This is an example of a 'bumpy' MLHFM after a number of closed loop corrections have been applied.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/08/mlhfm_lumpy.png "MLHFM bumpy")

First, use the 'Fit MLHFM' button to fit a polynomial of a user defined degree (I find a 6th degree polynomial to work well) to the 'bumpy' MLHFM. This will produce a fitted, smooth curve to your MLHFM. Copy that curve to your binaries MLHFM.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/08/mlhfm_smoothed.png "MLHFM smoothed")

* Get [ME7Logger](http://nefariousmotorsports.com/forum/index.php/topic,837.0title,.html)
* Log RPM (nmot), STFT (fr_w), LTFT (fra_w), MAF Voltage (uhfm_w), Throttle Plate Angle (wdkba) and Lambda Control Active (B_lr).
* Log long periods of consistent throttle plate angles and boost. We are trying to capture data where the MAF's rate of change (delta) is as small as possible. You don't have to stop/start logging between peroids of being consistent since ME7Tuner will filter the data for you, but you still want as much of this data as possible.
* Stay out of open-loop fueling. We don't care about it (right now). Like inconsistent MAF deltas, ME7Tuner will filter out open-loop data.
* Get at least 30 minutes of driving on a highway. Vary gears and throttle positions often to get measurements at as many throttle angles and RPM combinations as possible. Finding a highway with a long, consistent incline is ideal since you can 'load' the engine resulting in higher MAF voltages without going into open-loop fueling. Remember to slowly roll on and off the throttle. Sudden changes will result in less usuable data.
* Get at least 30 minutes of typical 'city' driving. Stop lights, slower city speeds, lots of gears and throttle positions. Remember to be as consistent as possible rolling on and off of the throttle.
* Get at least 15 minutes of parking lot data. Drive slowly around the parking lot in 1st and 2nd gear. Stop and start often. Vary the throttle plate and RPM as much as possible.
* Save your log and put it into a directory (along with other closed-loop logs from the same tune if desired).
* If you haven't done so already, create a .csv file of your MLHFM with headers of "voltage" and "kg/hr" and the corresponding values under each header. [Example mlhfm.csv](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/mlhfm.csv)
* Open ME7Tuner and click on the "Close Loop Fueling" tab at the top
* Click the KFKHFM tab on the left and copy your KFKHFM into the table.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/08/load_kfkhfm.png "KFKHFM")

* Click the "ME7 Logs" tab on the left hand side of the screen and click the "Load Logs" button at the bottom. Select the directory that contains your closed loop logs from ME7Logger. The derivative (dMAFv/dt) of the logged MAF voltages should plot on the screen. The vertical lines represent clusters of data at different derivative (rates of change, delta, etc...) for a given load and rpm. You want to select the data under the smallest derivative possible while also including the largest voltage range as possible. I find 1 to be a good derivative to start with.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/08/kfkhfm_closed_loop_samples.png "KFKHFM Close Loop Samples Std Dev")

* Click "Configure Filter" in the bottom left hand corner of the screen. This is where you can configure the filter for the incoming data. You can filter data by a minimum throttle angle, a minimum RPM, a maximum derivative (as discussed 1 is usually a good start).

* Click the "Correction" tab on the left side of the screen. You will see the corrected KFKHFM in the table that can be copied directly into TunerPro.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/08/corrected_kfkhfm.png "Corrected closed loop KFKHFM")

* Click the "Std Dev" tab at the bottom of the screen. This displays the derivative of the filtered data used to calcuate the corrections. Remember that a smaller derivative is better because the MAF's rate of change smaller (more stable).

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/08/sampled_corrections_kfkhfm.png "Filtered Closed Loop Std Dev")

* Click the "AFR Correction %" tab at the bottom of the screen. This displays the raw point cloud of Correction Errors with the Mean, Mode and Final AFR correction plotted ontop of the point cloud. Note how noisy the Correction Errors are.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/08/afr_corrections_kfkhfm.png "Filtered Closed Loop AFR Corection%")

* Load the corrected KFKHFM into a tune, take another set of logs and repeat the process until you are satisfied with your STFT/LTFT at idle and part throttle.

# MLHFM - Open Loop

Before attempting to tune open loop fueling, you really need to have KRKTE and and closed loop fueling nailed down. You also need a wide-band O2 sensor that is pre-cat. A tail sniffer likely isn't sufficient here.

Note that ME7Tuner is designed to be used with Zeitronix logs, but logs from any wideband can be modified to use the expected headers.

### Algorithm

This algorithm is roughly based on [mafscaling](https://github.com/vimsh/mafscaling/wiki/How-To-Use).

The error from estimated airflow based on measured AFR + STFT + LTFT at each voltage for MLHFM are calculated and then applied to the transformation.

The raw AFR is calculated as wideband **AFR / ((100 - (LTFT + STFT)) / 100)**. 

The AFR % error is calculated as **(raw AFR - interpolated AFR) / interpolated AFR * 100)**, where interpolated AFR is interpolated from **(raw AFR - ECU Target AFR) / ECU Target AFR * 100)**.

The corrected kg/hr transformation for MLHFM is calculated as current_kg/hr * ((AFRerror% / 100) + 1).

### Usage

Unlike closed loop corrections, open loop logs must be contained a single ME7Logger file and a single Zeitronix log. Both ME7Logger and Zeitronix logger need to be started before the first pull and stopped after the last pull. ME7Tuner correlates the ME7Logger logs and Zeitronix logs based on throttle position so both sets of logs need to contain the same number of pulls.

* Get [ME7Logger](http://nefariousmotorsports.com/forum/index.php/topic,837.0title,.html)
* Log RPM (nmot), STFT (fr_w), LTFT (fra_w), MAF Voltage (uhfm_w), Throttle Plate Angle (wdkba), Lambda Control Active (B_lr), MAF g/sec (mshfm_w), Requested Lambda (lamsbg_w) and Fuel Injector On-Time (ti_bl).
* Start both ME7Logger and the Zeitronix Logger and do as many WOT pulls as possible. Do pulls in 2nd and 3rd gear from 2000 RPM if possible. Stop both loggers when you are finished.
* Save your logs and put them into a directory
* If you haven't done so already, create a .csv file of your MLHFM with headers of "voltage" and "kg/hr" and the corresponding values under each header. [Example mlhfm.csv](http://kircherelectronics.com/wp-content/uploads/2019/02/mlhfm.csv)
* Open ME7Tuner and click on the "Open Loop Fueling" tab at the top
* Click the MLFHM tab on the left and click the "Load MLHFM" button and select your mlhfm.csv file. The file should load and plot.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-2.52.22-PM.png "MLHFM")

* Click the "ME7 Logs" tab on the left side of the screen.
* Click "Load ME7 Logs" and select the ME7Logger .csv file
* Click "Load AFR Logs" and select the Zeitronix .csv file
* You should see the requested AFR from ME7 plotted in blue and the actual AFR from Zeitronix in red. If the requested AFR doesn't match the actual AFR the MAF scaling is incorrect.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-3.55.57-PM.png "Open Loop Fueling")

* Click the "Airflow" tab at the bottom of the screen. You will see the airflow measured by the MAF in blue and the estimated airflow from the AFR in red. The measured airflow and estimated airflow should be the same or there the MAF scaling is incorrect.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-3.59.26-PM.png "Open Loop Airflow")

* Click the "Configure" filter button in the bottom left of the screen. You can see the minimum throttle angle, minimum RPM, minimum number of points from ME7Logger to count as a pull, the minimum number of points from Zeitronix to count as a pull and a maximum AFR. Note that Zeitronx can log at 40Hz while ME7Logger is usually 20Hz, so you may need to think about the number of points if your logging frequency is different.

* Click the "Correction" tab on the left side of the screen. You will see the current MLHFM plotted in blue and the corrected MLHFM plotted in red. The corrected MLHFM is also displayed in a table on the right hand side of the screen and can be copied directly into TunerPro. Clicking "Save MLFHM" will allow you to save MLFHM to a .csv file which can be loaded for the next set of corrections.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-4.04.49-PM.png "Open Loop MLHFM Correction")

* Click the "AFR Correction %" tab at the bottom of the screen. This displays the raw point cloud of Correction Errors with the Mean, Mode and Final AFR correction plotted ontop of the point cloud. Note how noisy the Correction Errors are.

![alt text](http://kircherelectronics.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-4.06.34-PM.png "Open Loop MLHFM AFR Correction%")

* Load the corrected MLHFM into a tune, take another set of logs and repeat the process until you are satisfied with your AFR at WOT.

* You may notice MLHFM starting to become 'bumpy' or 'not smooth' (for lack of a better term). This could be due to non-linearities in airflow due to changes in airflow velocity, but it is likely just noise we want to get rid of.  ME7Tuner has an option to fit your curve to a polynomial of a user configurable degree which will "smooth" your curve. Click the "Fit MLHFM" button with a reasonable polynomial degree (I find a 6th degree function to work well) to smooth your curve.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/MLFHM_POLYNOMIAL_FIT_OPEN_LOOP.png "Open Loop Polynomial Fit")

# KFMIRL (Torque request to Load/Fill request)

If you are using sigificantly more airflow than a stock engine you will need to modify KFMIRL in some way since your newly calibrated MAF has probably increased actual load (rl_w). How you modify is subjective and up to you, but I have provided a KFMIRL generator that I find useful. Don't forget to address LDRXN as well. Remeber that if you are running a stock MAP without the 5120 hack you will risk maxing out ps_w.

### Algorithm

The algorithm takes an **absolute** pressure request (don't forget to account for your altitude!) and KFURL (volumetric efficiency) to estimate a desired maximum load. The algorithm builds in some overhead like the stock KFMIRL seems to have. It also scales the load request in the same way that the stock KFMIRL does. You can choose the minimum torque request at which to apply the new load request. I find a minimum of 70%-80% torque request to be ideal. In general, you want to keep KFMIRL stock in all areas reachable under closed loop fueling. Use LDRXN to limit torque request in areas reachable under open loop fueling.

### Usage

* Enter your desired absolute pressure request
* Select a value for KFURL (volumetric efficiency) that is typical for open loop WOT
* Select a minimum torque request to apply the calcuated load request
* Copy paste KFMIRL directly into TunerPro

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFMIRL.png "KFMIRL")

# KFMIOP (Load/Fill to Torque)

If you modify you KFMIRL you want to regenerate KFMIOP as the inverse of KFMIRL to keep torque monitoring sane.

### Algorithm

The algorithm simply creates the inverse of KFMIRL to generate KFMIOP and provides a new KFMIOP axis. Don't forget to interpolate KFZWOP/KFMDS if you change KFMIOP axis. Note that simply generating the inverse is not enough and KFMIOP will need more work to get the torque monitoring right. KFMIRL and KFMIOP technically have nothing to do with each other, but using the inverse of KFMIRL to generate KFMIOP seems to be a good starting point.

* Read [Torque Monitoring](https://s4wiki.com/wiki/Tuning#Torque_monitoring)

Specifically these two points:

* Any part of KFMIOP (load/RPM range) that can only be reached above ~60% wped_w is unrestricted and can be raised to keep mimax high such that requested load does not get capped.
* Ensure that mibas remains below miszul to avoid intervention (which you will see in mizsolv) by lowering KFMIOP in areas reachable by actual measured load. This mainly becomes a concern in the middle of KFMIOP.

### Useage

* Copy and paste your KFMIRL and the algorithm will generate KFMIOP
* Copy and paste KFMIOP directly into TunerPro.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFMIOP.png "KFMIOP")

Note that KFMIOP also produces axes for KFMIOP, KFZWOP and KFZW so you can scale your ignition timing correctly.

# KFZWOP (Optimal Ignition Timing)

If you modified KFMIRL/KFMIOP you will want to modify the table and axis of KFZWOP to reflect to the new load range.

### Algorithm

The input KFZWOP is extrapolated to the input KFZWOP x-axis range (engine load from generated KFMIOP).

*Pay attention to the output!* Extrapolation is useful, but can be very stupid. Especially if the inputs have certain characteristics. Examine the output and make sure it is reasonable before using it. You will probably have to 'massage' the output a bit. The 3D charts can be very useful here.

### Useage

* Copy and paste your KFZWOP and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZWOP directly into TunerPro.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFZWOP.png "KFZWOP")

# KFZW/2 (Ignition Timing)

If you modified KFMIRL/KFMIOP you will want to modify the table and axis of KFZW/2 to reflect to the new load range.

### Algorithm

The input KFZW/2 is extrapolated to the input KFZW/2 x-axis range (engine load from generated KFMIOP).

*Pay attention to the output!* Extrapolation is useful, but can be very stupid. Especially if the inputs have certain characteristics. Examine the output and make sure it is reasonable before using it. You will probably have to 'massage' the output a bit. The 3D charts can be very useful here.

### Useage

* Copy and paste your KFZW/2 and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZW/2 directly into TunerPro.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFZW.png "KFZW")

# KFURL (VE)

If you have scaled your MAF correctly and your load is reasonable, KFURL should take minimal tweaking. If you are approaching values much greater than 0.11 or less than 0.9 you need to revisit your MAF scaling.

### Algorithm

You will need a pressure sensor that you can log in the intake manifold to correct KFURL. ME7Tuner is intented to use Zeitronix logs.

The average corrections are calculated as the difference between ps_w (calculated pressure in the intake manifold) and a second pressure sensor mounted in the intake manifold. Corrections are only taken in places where the throttle plate is more than 80% open. For currently unknown reasons, applying the correction in partial or closed throttle situations isn't producing corrections that correspond well to WOT situations.

Note that the correction is calculated and applied as a constant across the ignition advance (meaning only RPM is considered and ignition advance is ignored). This is done because getting enough samples for all ignition advance and RPM combinations isn't possible and the table starts looking strange.

### Useage

All logs must be contained a single ME7Logger file and a single Zeitronix log. Both ME7Logger and Zeitronix logger need to be started before the first pull and stopped after the last pull. 

* Log RPM (nmot), Barometric Pressure (pvdks_w) and Absolute Manifold Pressure (ps_w) along with the Zeitronix

* Get as many WOT pulls starting from as low as an RPM as possible to as high as an RPM as possible.

* Copy KFURL into the KFURL table.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFURL_INPUT.png "KFURL Input")

* Load the ME7 Logs
* Load the Zeitronix Logs
* Check the time alignment of the logs! The Zeitronix logs can't be stopped and started (it can only be started once) or the time alignment will fail.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFURL_LOGS.png "KFURL Logs")

* The corrected KFURL will be output in the Corrected KFURL table. These can be copy/pasted into TunerPro.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFURL_OUTPUT.png "KFURL Output")

* The corrections can be viewed. Pay attention to where there were enough samples to calculate a correction as you may need to interpret/guess the corrections in areas where there were not enough samples.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFURL_CORRECTION.png "KFRUL Corrections")

# WDKUGDN (Alpha-N Fueling)

With larger turbos you will likely need to adjust WDKUGDN so airflow at the throttle plate can be predicted accurately.

### Algorithm

The correction is calculated based on the difference between the mass airflow measured at the MAF (mshfm_w) and the airflow calculated at the throttle plate (msdk_w) when the throttle plate is more than 80% open.

### Useage

* Log RPM (nmot), Throttle Plate Angle (pvdks_w), Airflow from MAF (mshfm_w) and Airflow at Throttle Plate (msdk_w)

* Get as many WOT pulls starting from as low as an RPM as possible to as high as an RPM as possible.

* Copy WDKUGDN into the WDKUGDN table.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/WDKUDGN_OUTPUT.png "WDKUGDN Input")

* Load the ME7 Logs

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/WDKUDGN_LOGS.png "WDKUGDN Logs")

* The corrected WDKUGDN will be output in the Corrected WDKUGDN table. These can be copy/pasted into TunerPro.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/WDKUDGN_INPUT.png "WDKUGDN Output")

* The corrections can be viewed. Pay attention to where there were enough samples to calculate a correction as you may need to interpret/guess the corrections in areas where there were not enough samples.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/WDKUDGN_CORRECTIONS.png "WDKUGDN Corrections")

# KFWDKMSN (Alpha-N Fueling)

For cases where you need to calculate a new inverse from KFMSNWDK.

### Useage

* Paste KFMSNWDK into the KFMSNWDK Table
* The inverse map will be output in the KFWDKMSN Table.

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/KFWDKMSN.png "KFWDKMSN Corrections")

# LDRPID (Feed-Forward PID)

Provide a feed-forward (pre-control) factor to the existing PID. Highly recommended. The linearization process can be a lot of work. ME7Tuner can do most of the work for you. You just need to provide the logs.

Read [Actual pre-control in LDRPID](http://nefariousmotorsports.com/forum/index.php?topic=12352.0title=)

### Algorithm

The algorithm is mostly based on [elRey's algorithm](http://nefariousmotorsports.com/forum/index.php?;topic=517.0). However, instead of using increments to build the linearization table, ME7Tuner uses a fit one-dimensional polynomial which can (and likely will) produce better results. ME7Tuner can also parse millions of data points to produce the linearization table versus the handful of points you would get from doing it by hand.

### Useage

* Log RPM (nmot), Barometric Pressure (pvdks_w) and Absolute Manifold Pressure (ps_w), Throttle Plate Angle (pvdks_w), Wastegate Duty Cycle (ldtvm), and Selected Gear (gangi)

* Get as many WOT pulls starting from as low as an RPM as possible to as high as an RPM as possible. You will want a mix of "fixed" duty cycles and "real world" duty cycles.

* Put all of your logs in a single directory and load select the directory in ME7Tuner with "Load ME7 Logs"

* Wait awhile. It can take some time to parse the logs.

* The linearized duty cycle will be output in KFLDRL. Note that it may not be perfect and will likely take some additional massaging to get it right.

* For feed forward pre-control, ME7Tuner will output a new KFLDIMX and x-axis based on estimations from the linearized boost table. Keep in mind that this is just a ballpark estimation and will also likely require some massaging.

* I would reccomend requesting 95% duty cycle at any RPM ranges that can't produce the minimum boost required for cracking the wastegates (because you might as well be spooling as hard as you can here).

![alt text](http://kircherelectronics.com/wp-content/uploads/2020/10/LDRPID.png "ME7Tuner")
