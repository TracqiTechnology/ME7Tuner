# General

ME7Tuner is software that provides tools to help calibrate the MAF, primary fueling and torque/load requests. It is somewhat specific to an ME7 M-box ECU.

[Start with the S4 MAF Wiki](https://s4wiki.com/wiki/Mass_air_flow)

In any MAF application it may be necessary to increase the diameter of the MAF housing to extend the range of the sensor (while also reducing resolution).

In general, this is accomplished by applying a constant correction to the curve (MLHFM) that defines the conversion between the MAF sensors voltage output to an estimation of airflow. This constant correction is usually based on the change in diamater from the current MAF housing to the new MAF housing.

Non-linearities in the intake airflow and fuel system are then compenstated via KFKHFM and FKKVS.

* [See Primary Fueling](https://s4wiki.com/wiki/Tuning#Primary)
* [See Effect on Airflow](https://s4wiki.com/wiki/Mass_air_flow)

My experience with this approach when significantly increasing the diamater of the MAF housing (83mm housing to a 100mm housing) was that ot did not result in an optimal curve. While values that were reachable under closed-loop conditions had expected values, the values that were reachable under open-loop conditions (part and wide-open throttle) were notably underscaled.

### Example of MAF underscaling

* Same fuel system
* Same KRKTE (primary fueling)
* Same 100mm housing w/Hitachi sensor
* Same boost
* Same fuel
* Same weather
* Solid Line - Scaling based on a constand derived from the change in housing diameter
* Broken Line - Estimated airflow based on fuel consumption and air-fuel ratio

You can see a 100mm housing scaled with a constant based on a diameter increase (solid lines) vs a relatively accurate estimation of airflow (broken lines). Note that at lower values of airflow the measurements are similar while at higher values of airflow there is a significant discrepancy.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/100mmHitachi_vs_hpx.png "Underscaled 100mm housing")

The result of scaling the MAF based on a constand derived from the change in housing diameter was low LTFT (long-term fuel trims) corrections at idle and significant LTFT corrections at partial throttle. In other words, the car would idle fine at a lambda of 1, but WOT (wide open throttle) actual fueling lambda was lean compared to requested fueling lambda. Presumably, this leads to wildly a different KFKHFM and/or FKKVS compared to stock to compenstate for lean open-loop fueling.

### Example of MAF underscaling

The PMAS HPX slot sensor comes with a transfer function which I also found to be underscaled. This [Nefarious Motosports topic](http://nefariousmotorsports.com/forum/index.php?topic=382.0) also provides what is presumably an older version of the transfer function. I found both transfer functions to be underscaled in the open-loop areas similiar to the 100mm housing with a Hitachi sensor.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/hpx_curve.png "Underscaled 87mm housing")

### Summary of Examples

The underscaled transfer functions can possibly be attributed to the specific properties of my open element intake or to any number of other factors. The point is simply that you may want to calibrate your MAF to avoid large corrections in KFKHFM and/or FKKVS.

# (KRKTE) Primary Fueling

* Read [Primary Fueling](https://s4wiki.com/wiki/Tuning#Primary) first

The first step is to calculate a reasonable value for KRKTE (primary fueling). The is the value that allows the ECU to determine how much fuel is required to achieve a given AFR (air fuel ratio) based on a requested load/cyclinder filling. It is critial that KRKTE is close to the calculated value. If your KRKTE deviates significantly from the calculated value, your MAF is likely over/under scaled.

Pay attention to the density of gasoline (Gasoline Grams per Cubic Centimeter). The stock M-box assumes a value of 0.71 g/cc^3, but the [generally accepted density of gasoline](https://www.aqua-calc.com/page/density-table) is 0.75 g/cc^3. Also consider that ethanol has a density of 0.7893 g/cc^3 so high ethanol blends can be even denser. 

The KRKTE tab of ME7Tuner will help you calculate a value for KRKTE. Simply fill in the constants with the appropriate values.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-1.36.38-PM.png "Primary Fueling (KRKTE)")


When you are satisfied with KRKTE, you will need to get your MAF ballpark scaled to the new KRKTE. In my experience, applying the percentage change of KRKTE (from the previous value to the new value) to MLHFM works well enough. For example, if KRKTE is changed by 10% then change all of MLFHM by 10%. Or, if you have a transfer function that is fairly accurate, transfering those values to MLFHM should be all you need.

# Closed Loop

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
* Click the MLFHM tab on the left and click the "Load MLHFM" button and select your mlhfm.csv file. The file should load and plot.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-2.52.22-PM.png "MLHFM")

* Click the "ME7 Logs" tab on the left hand side of the screen and click the "Load Logs" button at the bottom. Select the directory that contains your closed loop logs from ME7Logger. The standard deviation of the logged MAF voltages should plot on the screen. The vertical lines represent clusters of data at different standard deviations (rates of change, delta, etc...) for a given MAF voltage. You want to select the data under the smallest standard deviation possible while also including the largest voltage range as possible. I find 0.15 to be a good standard deviation to start with.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-2.59.31-PM.png "Closed Loop Std Dev")

* Click "Configure Filter" in the bottom left hand corner of the screen. This is where you can configure the filter for the incoming data. You can filter data by a minimum throttle angle, a minimum RPM, a maximum standard deviation (as discussed 0.15 is usually a good start) and a window size to calculate the standard deviation (ME7Logger is usually 20Hz, so a window size of 20 results in a window of one second. A window size of 40 would be window of two seconds.).

* Click the "Correction" tab on the left side of the screen. You will see the current MLHFM plotted in blue and the corrected MLHFM plotted in red. The corrected MLHFM is also displayed in a table on the right hand side of the screen and can be copied directly into TunerPro. Clicking "Save MLFHM" will allow you to save MLFHM to a .csv file which can be loaded for the next set of corrections.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-3.08.05-PM.png "Corrected closed loop MLHFM")

* Click the "Std Dev" tab at the bottom of the screen. This displays the standard deviation of the filtered data used to calcuate the corrections. Remember that a smaller standard deviation is better because the MAF's rate of change smaller (more stable).

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-3.13.26-PM.png "Filtered Closed Looop Std Dev")

* Click the "AFR Correction %" tab at the bottom of the screen. This displays the raw point cloud of Correction Errors with the Mean, Mode and Final AFR correction plotted ontop of the point cloud. Note how noisy the Correction Errors are.

* Load the corrected MLHFM into a tune, take another set of logs and repeat the process until you are satisfied with your STFT/LTFT at idle and part throttle.

# Open Loop

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
* If you haven't done so already, create a .csv file of your MLHFM with headers of "voltage" and "kg/hr" and the corresponding values under each header. [Example mlhfm.csv](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/mlhfm.csv)
* Open ME7Tuner and click on the "Open Loop Fueling" tab at the top
* Click the MLFHM tab on the left and click the "Load MLHFM" button and select your mlhfm.csv file. The file should load and plot.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-2.52.22-PM.png "MLHFM")


