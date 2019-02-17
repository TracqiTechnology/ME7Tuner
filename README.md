# General

[Start with the S4 MAF Wiki](https://s4wiki.com/wiki/Mass_air_flow)

In any MAF application it may be necessary to increase the diameter of the MAF housing to extend the range of the sensor (while also reducing resolution).

In general, this is accomplished by applying a constant correction to the curve (MLHFM) that defines the conversion between the MAF sensors voltage output to an estimation of airflow. This constant correction is usually based on the change in diamater from the current MAF housing to the new MAF housing.

Non-linearities in the intake airflow and fuel system are then compenstated via KFKHFM and FKKVS.

* [See Primary Fueling](https://s4wiki.com/wiki/Tuning#Primary)
* [See Effect on Airflow](https://s4wiki.com/wiki/Mass_air_flow)

My experience with this approach when significantly increasing the diamater of the MAF housing (83mm housing to a 100mm housing) did not result in an optimal curve. While values that were reachable under closed-loop conditions had expected values, the values that were reachable under open-loop conditions (wide-open throttle) were notably underscaled.

## Example

You can see a 100mm housing scaled with a constant based on a diameter increase (solid lines) vs a relatively accurate estimation of airflow (broken lines). Note that at lower measurements of airflow the measurements are similar while at higher measurements of airflow there is a significant discrepancy.

![alt text](http://kircherelectronics.com.23.38-89-161.groveurl.com/wp-content/uploads/2019/02/100mmHitachi_vs_hpx.png "Underscaled 100mm housing")


