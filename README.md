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

You can see a 100mm housing scaled with a constant based on a diameter increase (solid lines) vs a relatively accurate estimation of airflow (broken lines). Note that at lower measurements of airflow the measurements are similar while at higher measurements of airflow there is a significant discrepancy.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/100mmHitachi_vs_hpx.png "Underscaled 100mm housing")

The result of scaling the MAF based on a constand derived from the change in housing diameter was low LTFT (long-term fuel trims) corrections at idle and significant LTFT corrections at partial throttle. In other words, the car would idle fine at a lambda of 1, but WOT (wide open throttle) actual fueling lambda was lean compared to requested fueling lambda. Presumably, this leads to wildly a different KFKHFM and/or FKKVS compared to stock to compenstate for lean open-loop fueling.

### Example of MAF underscaling

The PMAS HPX slot sensor comes with a transfer function which I also found to be underscaled. This [Nefarious Motosports topic](http://nefariousmotorsports.com/forum/index.php?topic=382.0) also provides what is presumably and older version of the transfer function. I found both transfer functions to be underscaled in the open-loop areas similiar to the 100mm housing with a Hitachi sensor.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/hpx_curve.png "Underscaled 87mm housing")

### Summary of Examples

The underscaled transfer functions can possibly be attributed to the specific properties of my open element intake or to any number of other factors. The point is simply that you may want to calibrate your MAF to avoid large corrections in KFKHFM and/or FKKVS.

# (KRKTE) Primary Fueling

* Read [Primary Fueling](https://s4wiki.com/wiki/Tuning#Primary) first

The first step is to calculate a reasonable value for KRKTE (primary fueling). The is the value that allows the ECU to determine how much fuel is required to achieve a given AFR (air fuel ratio) based on a requested load/cyclinder filling. It is critial that KRKTE is close to the calculated value. If your KRKTE deviates significantly from the calculated value, your MAF is likely over/under scaled.

Pay attention to the density of gasoline (Gasoline Grams per Cubic Centimeter). The stock M-box assumes a value of 0.71 g/cc^3, but the [generally accepted density of gasoline](https://www.aqua-calc.com/page/density-table) is 0.75 g/cc^3. Also consider that ethanol has a density of 0.7893 g/cc^3 so high ethanol blends can be even denser. 

The KRKTE tab of ME7Tuner will help you calculate a value for KRKTE. Simply fill in the constants with the appropriate values.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/Screen-Shot-2019-02-17-at-1.36.38-PM.png "Primary Fueling (KRKTE)")






