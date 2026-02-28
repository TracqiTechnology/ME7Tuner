# ME7Tuner Calibration Guide

This is the companion document to the [README](../README.md#stage-2-calibration). Where the README tells you *what* each tool does and *whether* you need it, this guide tells you *how* — step by step, with screenshots, algorithms, and the kind of detail that only matters when you're actually doing the work.

It's organized in calibration order. Start at the top and work down. Skip sections that don't apply to your hardware. If you're not sure whether a section applies, it probably doesn't — but read it anyway, because understanding how ME7 models torque will save you from doing something regrettable later.

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

Your MAF sensor is a liar. Not intentionally — it's a hot-wire anemometer trying to measure chaotic airflow through a tube, and it has 512 voltage bins to do it with. Over time, contamination, housing changes, and thermal drift all conspire to make the linearization curve (MLHFM) inaccurate. Closed loop correction fixes this by using the narrowband O2 sensors and fuel trims to figure out where the MAF is lying and by how much.

This algorithm is roughly based on [mafscaling](https://github.com/vimsh/mafscaling/wiki/How-To-Use).

### Algorithm

The LTFT and STFT corrections at each voltage for MLHFM are calculated and then applied to the transformation.

The Correction Error at each measured voltage is calculated as **(STFT - 1) + (LTFT - 1)**, scaled by the ratio of the logged voltage to the nearest MLHFM voltage (**logged_voltage / nearest_MLHFM_voltage**). Voltages with fewer than 5 samples receive no correction.

The Total Correction is the average of the mean and mode of the Correction Errors at each measured voltage for MLHFM.

A 5-point moving average is applied to smooth the corrections before the optional polynomial fit.

The corrected kg/hr transformation for MLHFM is calculated as **current_kg/hr * (tot_corr + 1)**.

### Usage

The key to closed loop correction is data volume. Narrowband O2 sensors are noisy, slow, and only accurate near stoichiometry — so the algorithm depends on averaging hundreds of samples per voltage bin to estimate the real correction. More data, better results. There are no shortcuts here.

The Closed Loop parser is designed to process multiple log files at once, so you can accumulate logs over days or weeks. The only rule: your tune and hardware cannot change between logs. It also helps to log in consistent weather — temperature and humidity affect air density, and you don't want that showing up as a MAF error.

* Get [ME7Logger](http://nefariousmotorsports.com/forum/index.php/topic,837.0title,.html)

Log the following parameters:

* RPM - 'nmot'
* STFT - 'fr_w'
* LTFT - 'fra_w'
* MAF Voltage - 'uhfm_w'
* Throttle Plate Angle - 'wdkba'
* Lambda Control Active - 'B_lr'
* Engine Load - 'rl_w' (required by parser, not used in correction algorithm)

#### Logging Instructions

75 minutes of driving sounds like a lot. It is. Your MAF has 512 voltage bins and you need meaningful sample counts at every one of them. Here's how to fill those bins:

* **Highway (30+ minutes):** Vary gears and throttle positions constantly. You're trying to sweep as many MAF voltage / RPM combinations as possible without going into open-loop fueling. If you can find a highway with a long, consistent incline — perfect. You can load the engine to higher MAF voltages without triggering WOT enrichment. Roll on and off the throttle *slowly*. Sudden changes create large dMAFv/dt values that get filtered out.

* **City driving (30+ minutes):** Stop lights, slower speeds, lots of gear changes and throttle positions. Same rules — be smooth and vary everything. Every traffic light is an opportunity for low-voltage data points.

* **Parking lot (15+ minutes):** Drive slowly in 1st and 2nd gear. Stop and start often. This fills in the low-voltage bins that highway driving misses. Yes, you will look strange driving in circles around a parking lot for 15 minutes. Your MAF calibration doesn't care about your dignity.

* You don't have to stop and start logging between driving styles — ME7Tuner filters the data for you. But you still want as much steady-state data as possible. The more consistent your throttle position at any given moment, the better.

Save your log and put it into a directory (along with other closed-loop logs from the same tune if desired).

#### In ME7Tuner

Navigate to the **Closed Loop** calibration tab.

<img src="/documentation/images/closed_loop_mlhfm.png" width="800">

Click **Load Logs** at the bottom of the **ME7 Logs** tab and select the directory containing your closed loop logs. The derivative (dMAFv/dt) of the logged MAF voltages will plot on screen. Those vertical clusters represent data at different rates of change for a given MAF voltage. You want to select data under the smallest derivative possible while still covering the widest voltage range. A derivative of 1 is usually a good starting point.

* Green samples are included by the filter
* Red samples are excluded by the filter

<img src="/documentation/images/closed_loop_mlhfm_filter.png" width="800">

Click **Configure Filter** to adjust the filter parameters — minimum throttle angle, minimum RPM, maximum derivative (start with 1).

Click the **Correction** tab and select the **MLHFM** sub-tab. You'll see the current MLHFM in blue and the corrected MLHFM in red. The corrected curve is also displayed in a table on the right that can be copied directly into TunerPro. Use **Write MLHFM** to write the corrected values back to the binary.

<img src="/documentation/images/closed_loop_mlhfm_corrected.png" width="800">

The **dMAFv/dt** sub-tab shows the derivative of the filtered data used to calculate corrections. Smaller derivatives are better — they mean the MAF's rate of change was low (more stable readings).

<img src="/documentation/images/closed_loop_mlhfm_derivative.png" width="800">

The **AFR Correction %** sub-tab shows the raw point cloud of Correction Errors with the Mean, Mode, and Final AFR correction plotted on top. Note how noisy the individual Correction Errors are — this is why you need so much data. The averaging is doing the heavy lifting.

<img src="/documentation/images/closed_loop_mlhfm_corrected_percentage.png" width="800">

#### Iterate

Load the corrected MLHFM into your tune, drive another set of logs, and repeat until your STFT/LTFT look clean at idle and part throttle.

If you notice MLHFM getting "bumpy" after several iterations — peaks and valleys that don't correspond to real airflow non-linearities — that's accumulated noise. ME7Tuner has a polynomial fit option for exactly this. On the **MLHFM** sub-tab, click **Fit MLHFM** with a reasonable degree (6th degree works well in practice) to smooth the curve while preserving the overall shape.

<img src="/documentation/images/closed_loop_mlhfm_corrected_best_fit.png" width="800">

---

## Open Loop MLHFM

Closed loop gets you accurate at part throttle. Open loop gets you accurate at WOT — which is where the MAF error actually matters for power and safety. If your AFR is 12.5:1 when ME7 thinks it's requesting 11.5:1, you have a fueling problem, and the narrowband O2 sensors that closed loop relies on can't see it. You need a wideband.

Before attempting open loop correction, you **need** KRKTE (primary fueling) and closed loop fueling nailed down. Garbage in, garbage out — if your part-throttle MAF curve is wrong, your open-loop corrections will be compensating for the wrong thing.

You also need a wideband O2 sensor that is pre-cat. A tail sniffer is not sufficient — the catalytic converter changes the AFR reading.

ME7Tuner is designed to work with Zeitronix logs, but logs from any wideband can be modified to use the expected headers. Open an issue with an example log file if you'd like other formats supported.

### Algorithm

This algorithm is roughly based on [mafscaling](https://github.com/vimsh/mafscaling/wiki/How-To-Use).

The error from estimated airflow based on measured AFR + STFT + LTFT at each voltage for MLHFM are calculated and then applied to the transformation.

The raw AFR is calculated as wideband **AFR / ((100 - (LTFT + STFT)) / 100)**.

The AFR % error is calculated as **(raw AFR - interpolated AFR) / interpolated AFR * 100)**, where interpolated AFR is interpolated from **(raw AFR - ECU Target AFR) / ECU Target AFR * 100)**.

The corrected kg/hr transformation for MLHFM is calculated as current_kg/hr * ((AFRerror% / 100) + 1).

### Usage

Unlike closed loop, open loop requires precise synchronization between two loggers. Each WOT pull in the ME7Logger file must correspond 1:1 with a pull in the Zeitronix file — same order, same count. Both loggers need to be running before the first pull and stopped after the last pull. ME7Tuner detects pull boundaries using throttle position, matches pulls by order, and correlates data points within each pull by RPM.

This means: start both loggers, do your pulls, stop both loggers. Don't start one logger, do a pull, start the other logger, and wonder why the pull counts don't match.

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

#### Logging Instructions

Start both ME7Logger and the Zeitronix Logger. Do as many WOT pulls as possible — 2nd and 3rd gear from 2000 RPM gives the best RPM sweep. More pulls means more data points across the voltage range, and more data means better corrections. Stop both loggers when finished.

Save your logs and put them into a directory.

#### In ME7Tuner

Navigate to the **Open Loop** calibration tab.

<img src="/documentation/images/open_loop_mlhfm.png" width="800">

On the **ME7 Logs** tab, use the buttons at the bottom to load your log files:
* **Load ME7 Logs** — select the ME7Logger .csv file
* **Load Zeitronix Logs** — select the Zeitronix .csv file

On the **Fueling** sub-tab, you should see the requested AFR from ME7 plotted in blue and the actual AFR from Zeitronix in red. *If the requested AFR doesn't match the actual AFR, the MAF scaling is incorrect.* That's the whole point — you're here to fix that.

<img src="/documentation/images/open_loop_mlhfm_logs.png" width="800">

The **Airflow** sub-tab shows MAF-measured airflow in blue and estimated airflow from AFR in red. These should overlap. If they don't, that's the error you're correcting.

<img src="/documentation/images/open_loop_mlhfm_airflow.png" width="800">

Click **Configure Filter** to adjust the filter — minimum throttle angle, minimum RPM, minimum points per pull for ME7Logger and Zeitronix, and maximum AFR. Note that Zeitronix logs at 40Hz while ME7Logger typically runs at 20Hz, so adjust your point thresholds accordingly.

Click the **Correction** tab. The **MLHFM** sub-tab shows the current MLHFM in blue and the corrected MLHFM in red. The table on the right can be copied directly into TunerPro. Use **Write MLHFM** to write the corrected values back to the binary.

<img src="/documentation/images/open_loop_mlhfm_correction.png" width="800">

The **AFR Correction %** sub-tab shows the raw Correction Error point cloud with Mean, Mode, and Final correction overlaid. Same story as closed loop — individual points are noisy, averaging does the work.

<img src="/documentation/images/open_loop_mlhfm_correction_percentage.png" width="800">

#### Iterate

Load the corrected MLHFM into your tune, take another set of logs, and repeat until your WOT AFR matches what ME7 is requesting. Same polynomial fit option as closed loop — use it if the curve gets noisy after multiple iterations.

<img src="/documentation/images/open_loop_mlhfm_correction_best_fit.png" width="800">

---

## PLSOL - Pressure to Load Conversion

PLSOL is your reality-check calculator. It answers the question every tuner eventually asks: "how much boost do I actually need to hit X% load?" — and, by extension, "can my hardware even do that?"

The model is deliberately simple. The *only* parameter that affects load is pressure. Barometric pressure, intake air temperature, and the pressure-to-load conversion factor (KFURL) are all treated as constants. This means the model says 16 psi is 16 psi no matter what turbo produces it — a K03 making 16 psi and a GT35R making 16 psi produce the same load at the same RPM.

Despite the simplicity, this model is remarkably consistent with real-world results. The reason it works is that ME7 *itself* uses this simplified model — so even if the physics are more complex, the ECU's behavior follows this math.

You can edit barometric pressure, intake air temperature, and the pressure-to-load conversion to see how ME7 would respond to these parameters changing in the real world.

Note that for 2.7L of displacement to approach 900 horsepower, the pressure required is approaching 3 bar (45 psi) relative and 4 bar (60 psi) absolute. At that point you've left "calibration" and entered "engineering project."

<img src="/documentation/images/plsol.png" width="800">

### PLSOL -> Airflow (Pressure to Airflow)

ME7Tuner calculates estimated airflow for a given load based on engine displacement (in liters) and RPM. This tells you whether your MAF housing can support the load you're requesting — because if the MAF can't measure the airflow, the ECU can't control it.

<img src="/documentation/images/plsol_airflow.png" width="800">

### PLSOL -> Power (Pressure to Horsepower)

ME7Tuner calculates estimated horsepower for a given load based on engine displacement (in liters) and RPM.

<img src="/documentation/images/plsol_power.png" width="800">

---

## KFMIOP (Load/Fill to Torque)

KFMIOP is the optimum torque table — and it's the map that confuses more people than any other in ME7. Most of that confusion comes from the word "torque," because KFMIOP doesn't contain torque values in any physical unit. It contains *normalized* torque — a value between 0% and 100% that represents the ratio of current load to the maximum load the MAP sensor can see.

Understanding this normalization is the key to everything that follows. Get it right and the rest of the calibration tables fall into place. Get it wrong and you'll spend weeks chasing torque monitoring interventions that make no sense.

Note that KFMIRL is the inverse of KFMIOP, not the other way around.

### Tuning Philosophy

KFMIOP describes the optimum engine torque at each load/RPM point, but it does so as a normalization relative to the MAP sensor's maximum measurement capability.

"Torque" here is a value between 0 and 1 (or 0% and 100%). Each cell in KFMIOP represents: *at this load and RPM, what fraction of the MAP sensor's maximum theoretical load does optimum torque represent?*

When we look at KFMIOP for a B5 S4 (M-box) we see the table is built around a 2.5 bar (36 psi) absolute MAP sensor limit — the stock sensor's ceiling. Running 2.5 bar absolute through the PLSOL calculations yields ~215% load. That's the denominator for every cell in the table.

The maximum pressure a K03 can efficiently produce is about 1.0 bar (15 psi) relative / 2.0 bar absolute, which corresponds to ~191% load via PLSOL. On the stock M-box KFMIOP, the maximum load axis value is 191% and the maximum torque value is 89% — because 191/215 ≈ 0.89. Every column in KFMIOP maps itself to a normalized peak torque value defined by that 215% ceiling.

This is the part that breaks when you change hardware. If you upgrade to bigger turbos that can produce more than 2.5 bar absolute, you need a bigger MAP sensor — and the moment you change the MAP sensor limit, *the denominator changes for every cell in the table*. KFMIOP needs to be rescaled, not extrapolated.

ME7Tuner handles this by taking your new maximum MAP pressure, rescaling the load axis via PLSOL, and then renormalizing the torque values. For example, going from ~215% max load (2.5 bar) to ~400% max load (4 bar) means the torque request at the old 9.75% load column gets reduced from ~4% to ~2% — because with the wider MAP range, 9.75% load represents a proportionally smaller fraction of the maximum.

KFMIOP can also be converted to a boost table via the PLSOL calculation after you've derived peak load. Looking at the resulting boost table, the stock values appear to have been created empirically on an engine dyno and tuned specifically for stock hardware (K03 turbos). Unless you have access to a dyno, there's no way to derive OEM-quality KFMIOP for your specific hardware. ME7Tuner's rescaling is a principled approximation — better than extrapolating or just widening the axis and hoping for the best.

* Read [Torque Monitoring](https://s4wiki.com/wiki/Tuning#Torque_monitoring)

Additional empirical tuning points:

* Any part of KFMIOP (load/RPM range) that can only be reached above ~60% wped_w is unrestricted and can be raised to keep mimax high such that requested load does not get capped.
* Ensure that mibas remains below miszul to avoid intervention (which you will see in mizsolv) by lowering KFMIOP in areas reachable by actual measured load. This mainly becomes a concern in the middle of KFMIOP.

### KKK Compressor Map Reference

Every turbo upgrade decision ultimately comes back to one question: *how much boost can this compressor produce efficiently, and what does that mean for the ME7 calibration?*

The compressor maps below are from KKK (Kühnle, Kopp & Kausch — now BorgWarner) for every turbo commonly found on the 2.7T platform. The Y-axis is the pressure ratio (p2/p1) — outlet pressure divided by inlet pressure. The X-axis is corrected mass flow. The curved lines inside the efficiency island are isentropic efficiency contours (higher is better — 0.70+ is good, 0.73+ is excellent). The leftmost boundary is the surge line (compressor stalls — very bad). The rightmost boundary is the choke line (flow can't increase — the turbo is done).

For the 2.7T twin-turbo V6, each compressor feeds 3 cylinders of 2.7L total displacement, so you're looking at roughly half-engine flow rates on each map. Keep this in mind when reading the maps — the 2.7T operating line sits in a different spot than a single-turbo 4-cylinder using the same compressor.

#### K03 (Stock 2.7T) — KKK K03-1870 EXA

<img src="/documentation/images/compressor_maps/K03-1870_EXA.png" width="600">

The turbo that came on every B5 S4. Peak efficiency of ~73% at modest pressure ratios. The efficiency island extends to about PR 2.0 (1.0 bar relative / 2.0 bar absolute) before falling off rapidly. The K03 was designed for the stock power target (~265 hp from the pair) and runs out of breath quickly above 1.0 bar relative. At the 2.7T's flow rates, you can push slightly past PR 2.0, but you're falling off the efficiency cliff — compressor outlet temperatures rise, you lose intercooler margin, and the turbo shaft speed approaches its limit.

**ME7Tuner implications:** Stock MAP sensor (2.5 bar) is more than adequate. Maximum efficient load is ~191%, well within the stock KFMIOP range. The **Optimizer alone** is sufficient for this turbo — no KFMIOP/KFMIRL recalibration needed.

#### K04-2078 EYE (Common Upgrade)

<img src="/documentation/images/compressor_maps/K04-2078_EYE.png" width="600">

The bolt-on upgrade that launched a thousand forum builds. Same bearing housing and turbine as the K03 with a larger compressor wheel. The efficiency island is wider in both flow and pressure ratio, extending to about PR 2.5 (1.5 bar relative / 2.5 bar absolute) at 2.7T flow rates. Peak efficiency is ~73% over a broader range than the K03.

**ME7Tuner implications:** The stock 2.5 bar MAP sensor is *exactly* at the limit — 2.5 bar absolute is the sensor ceiling and the K04-2078's efficient limit simultaneously. This is no coincidence — the K04 was designed as a factory upgrade (RS4 used a variant). At 2.5 bar you're at ~215% load, which is the top of the stock KFMIOP axis. You can get away with **Optimizer only** for most K04-2078 builds, but if you're pushing maximum boost on warm days, you may want to rescale KFMIOP to give yourself headroom. Borderline — your call.

#### K04-0025

<img src="/documentation/images/compressor_maps/K04-0025.jpg" width="600">

A larger variant in the K04 family — same speed lines as the K04-2078 but with the efficiency island extending further. Usable pressure ratios approach PR 2.8 (1.8 bar relative / 2.8 bar absolute) at 2.7T flow rates before efficiency drops below 65%. This is the turbo that forces you past the stock MAP sensor.

**ME7Tuner implications:** The stock 2.5 bar MAP sensor **cannot see** what this turbo can produce. You need a 3 bar sensor minimum. Load range extends to ~240%, which means the stock KFMIOP table with its 191% load ceiling is completely inadequate. **Full calibration required** — KFMIOP rescale, KFMIRL inversion, KFZWOP/KFZW axis extension, and probably a MAP sensor upgrade.

#### K14 — KKK 2464

<img src="/documentation/images/compressor_maps/K14-2464.jpg" width="600">

Moving into the physically larger turbo frames. The K14 is a genuine step up from the K04 — larger compressor and turbine wheels in a larger housing. The compressor map shows a wider flow range with peak efficiency around 76%. On the 2.7T twin-turbo application, this compressor can produce pressure ratios around 2.5–3.0 (roughly 1.5–2.0 bar relative / 2.5–3.0 bar absolute) while staying within the efficiency island. The turbine side spools slower than a K04, but once it's lit, there's significantly more airflow available.

**ME7Tuner implications:** 3 bar MAP sensor minimum — and you'll be using most of that range. Load range around ~270%. **Full calibration required.** At this level you're also running into MAF housing limits on the stock 70mm housing — consider logging mshfm_w to verify you haven't maxed it.

#### K16 — KKK 2467

<img src="/documentation/images/compressor_maps/K16-2467.gif" width="600">

The K16 is where "upgrade" becomes "project car." This is a substantially larger compressor than the K14 with efficiency contours extending to pressure ratios above 2.5 on the 2.7T flow range. At the 2.7T's half-engine flow rates, you can push to approximately PR 3.2 (2.2 bar relative / 3.2 bar absolute) before running out of efficiency. The surge line is also further right, meaning you need more flow at low RPM to keep the compressor happy — expect more turbo lag than a K04 or K14.

**ME7Tuner implications:** 4 bar MAP sensor required — you'll exceed 3 bar absolute. Load range approaches ~300%. **Full calibration required**, and you're now in territory where injector size, fuel pump capacity, and MAF housing all become limiting factors in addition to the ECU calibration. The ME7Tuner Calibration workflow earns its keep at this level.

#### K24 — KKK 2470

<img src="/documentation/images/compressor_maps/K24-70gga.jpg" width="600">

The K24 (Verdichterkennfeld 2470) is a large-frame compressor originally used on industrial and commercial vehicle applications. The compressor map shows pressure ratios extending well past 3.0 with peak efficiency around 76%. At 2.7T flow rates, you can push to approximately PR 3.5 (2.5+ bar relative / 3.5 bar absolute) within the efficiency island. This is serious hardware — the turbine spool time is significant and the compressor needs substantial exhaust energy to reach its operating range.

**ME7Tuner implications:** 4 bar MAP sensor required. Load range around ~340%. **Full calibration required.** At this power level (~500+ hp from a 2.7T), the ECU calibration is just one piece of a much larger puzzle that includes fuel system, cooling, drivetrain, and the structural limits of the engine block itself. ME7Tuner handles the ECU side; the rest is between you and your engine builder.

#### K26 — KKK 2664

<img src="/documentation/images/compressor_maps/K26_map.jpg" width="600">

*Compressor map shown with Audi 5-cylinder fullload line for reference.*

The K26 is the turbo that powered the Audi 200 Turbo and the UrS4/UrS6 — a proven design with a wide efficiency island. The compressor map shows pressure ratios up to 3.0 with the fullload line (plotted for the 5-cylinder application) peaking around PR 2.0. On the 2.7T twin-turbo application, each K26 would be feeding 1.35L of displacement — well within the map's flow range — allowing you to push deep into the pressure ratio range.

#### K26 — KKK 2470 (Porsche 924 Turbo variant)

<img src="/documentation/images/compressor_maps/K26_map2.jpg" width="600">

*Compressor map shown with Porsche 924 Turbo fullload line for reference.*

A different K26 variant (2470 R) used on the Porsche 924 Turbo. Similar overall characteristics to the 2664 — wide efficiency island, pressure ratios up to 3.0+, peak efficiency in the 68–72% range. The fullload line for the Porsche 4-cylinder application peaks around PR 2.2–2.4. The 2.7T twin-turbo application would operate at lower flow rates per turbo, placing the operating line further left on the map.

**ME7Tuner implications (both K26 variants):** 4 bar+ MAP sensor required. Load range can exceed ~400%. **Full calibration required.** The K26 on a 2.7T is a big turbo build — expect significant spool time, require supporting mods (fuel system, intercooler, exhaust manifold), and plan for extensive dyno tuning beyond what any calculator can provide. ME7Tuner gets you a principled starting point for the ECU calibration.

#### RS2 — KKK 2672

<img src="/documentation/images/compressor_maps/Rs2_2672.jpg" width="600">

The RS2 turbo (Verdichterkennfeld 2672 GGCAA) — the compressor from the legendary Audi RS2 Avant. This is a high-flow, high-pressure-ratio design with the efficiency island extending past PR 3.0. At 2.7T flow rates, you can push to approximately PR 3.5 (2.5+ bar relative / 3.5 bar absolute) before running out of map. The surge line is moderate, meaning reasonable spool for the size, and peak efficiency sits around 71%.

**ME7Tuner implications:** 4 bar MAP sensor required. Load range around ~340%. **Full calibration required.** Similar territory to the K24 in terms of calibration complexity, but the RS2's historically available aftermarket support means more reference tunes to compare against.

#### Turbo → Calibration Decision Matrix

This table summarizes what each turbo means for your ME7 calibration. The boost and load values are approximate — read from the compressor maps for the 2.7T twin-turbo application, assuming standard atmospheric conditions and reasonable intercooler efficiency. Your results will vary with exhaust housing, intercooler, altitude, and IAT.

| Turbo | Max Efficient Boost | Max Load (approx) | MAP Sensor | Calibration Needed |
|-------|--------------------:|-------------------:|:----------:|:-------------------|
| **K03** (stock) | ~1.0 bar / 2.0 bar abs | ~191% | Stock 2.5 bar | Optimizer only |
| **K04-2078** | ~1.5 bar / 2.5 bar abs | ~215% | Stock 2.5 bar (at limit) | Optimizer + maybe KFMIOP |
| **K04-0025** | ~1.8 bar / 2.8 bar abs | ~240% | 3 bar minimum | Full calibration |
| **K14** | ~2.0 bar / 3.0 bar abs | ~270% | 3 bar+ | Full calibration |
| **K16** | ~2.2 bar / 3.2 bar abs | ~300% | 4 bar | Full calibration |
| **K24** | ~2.5+ bar / 3.5 bar abs | ~340% | 4 bar | Full calibration |
| **K26** | ~3.0+ bar / 4.0 bar abs | ~400%+ | 4 bar+ | Full calibration |
| **RS2** | ~2.5+ bar / 3.5 bar abs | ~340% | 4 bar | Full calibration |

The pattern is clear: once you exceed what the stock 2.5 bar MAP sensor can measure, you need the full Calibration workflow. Below that threshold, the Optimizer handles most of what you need. The K04-2078 is the inflection point — it's exactly at the stock MAP sensor limit, which is why it's such a popular upgrade. It's the most turbo you can bolt on without recalibrating the torque model.

### Usage

* On the left side, ME7Tuner analyzes the current KFMIOP and estimates the MAP sensor upper limit and the real-world pressure limit (usually turbo-limited) of the existing calibration
* Based on the analysis, a boost table is derived and viewable under the "Boost" tab
* On the right side, input your new MAP sensor upper limit and desired pressure limit
* ME7Tuner outputs a rescaled KFMIOP table and axis
* Copy the KFMIOP table and axis to other tables (KFMIRL/KFZWOP/KFZW) to generate corresponding maps

<img src="/documentation/images/kfmiop.png" width="800">

Note that KFMIOP also produces axes for KFMIOP, KFZWOP, and KFZW so you can scale your ignition timing correctly.

---

## KFMIRL (Torque Request to Load/Fill Request)

KFMIRL is the inverse of KFMIOP. It exists entirely as a lookup optimization — so the ECU doesn't have to search KFMIOP every time it wants to convert a torque request into a load request. If KFMIOP says "at this load and RPM, optimum torque is X%," then KFMIRL says "for X% torque at this RPM, the required load is Y%."

This means KFMIRL is mechanically derived from KFMIOP. If you change KFMIOP, you must regenerate KFMIRL or the ECU's torque-to-load conversion won't match its load-to-torque conversion. ME7 will notice this inconsistency and it will not be subtle about letting you know.

<img src="/documentation/images/kfmirl.png" width="800">

### Algorithm

Inverts the input for the output.

### Usage

* KFMIOP is the input and KFMIRL is the output
* KFMIOP from the binary will be displayed by default
* Optionally modify KFMIOP to the desired values
* KFMIOP will be inverted to produce KFMIRL on the left side

---

## KFZWOP (Optimal Ignition Timing)

If you modified KFMIRL/KFMIOP, you need to extend the KFZWOP axis to cover the new load range. KFZWOP defines the ignition advance at which the engine produces maximum torque (MBT) for each load/RPM point. If your load axis doesn't reach your actual operating range, the ECU is flying blind on timing.

<img src="/documentation/images/kfzwop.png" width="800">

### Algorithm

The input KFZWOP is extrapolated to the input KFZWOP x-axis range (engine load from generated KFMIOP).

**Pay very close attention to the output.** Extrapolation works reasonably for linear functions. Ignition timing as a function of load is *not* a linear function — at high loads, optimal timing decreases rapidly to avoid detonation, and a linear extrapolation will happily suggest advancing timing into knock territory. The math doesn't know this. You do. Examine the output and make sure it's physically reasonable before using it. You will almost certainly have to rework the high-load end of the output manually.

If you extrapolate ignition timing and flash it without checking, you deserve whatever happens next.

### Usage

* Copy and paste your KFZWOP and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZWOP directly into TunerPro

---

## KFZW/2 (Ignition Timing)

Same story as KFZWOP — if you modified KFMIRL/KFMIOP, you need to extend KFZW/2 to cover the new load range. KFZW defines the *actual* ignition timing map (as opposed to KFZWOP's *optimal* timing), so the same warnings about extrapolation apply — arguably more so, since KFZW is what the ECU actually fires the spark plugs with.

<img src="/documentation/images/kfzw.png" width="800">

### Algorithm

The input KFZW/2 is extrapolated to the input KFZW/2 x-axis range (engine load from generated KFMIOP).

**Pay very close attention to the output.** Same warning as KFZWOP — extrapolation of a non-linear function will produce garbage at the extremes. Optimal ignition advance does not increase linearly with load. At high loads, it decreases. A linear extrapolation doesn't know this and will suggest timing values that will detonate your engine. Check the output. Rework the high-load cells manually. This is not optional.

### Usage

* Copy and paste your KFZW/2 and the x-axis load range generated from KFMIOP
* Copy and paste the output KFZW/2 directly into TunerPro

---

## KFVPDKSD (Throttle Transition)

In a turbocharged ME7 application, the throttle isn't just an on/off switch — it's one half of a pressure control system. The ECU coordinates the throttle body valve and the turbo wastegate (N75 valve) to hit the requested manifold pressure. KFVPDKSD defines where the handoff happens.

Here's the logic: at any given RPM, the ECU checks whether the desired boost pressure can be achieved by the turbo. If it *can't* (low RPM, turbo hasn't spooled yet), the throttle opens to 100% to minimize restriction — get out of the turbo's way and let it spool. If it *can* (mid-to-high RPM, turbo is producing boost), the throttle closes to the choke angle defined in WDKUGDN, and the wastegate takes over pressure control.

The Z-axis of KFVPDKSD is a pressure ratio — effectively the transition zone in the last ~5% between atmospheric pressure (<1) and positive boost (>1). It marks where the pressure crosses from sub-atmospheric (~0.95) to super-atmospheric (~1.0+). In the region where the turbo can produce boost but hasn't reached the wastegate cracking pressure (the N75 is unresponsive), the throttle is used to keep boost under control at the requested limit.

<img src="/documentation/images/kfvpdksd.png" width="800">

### Algorithm

ME7Tuner parses a directory of logs and determines at what RPM points a given boost level can be achieved.

### Usage

You need a large set of logs — the more operating conditions covered, the more accurate the transition map.

Required Logged Parameters:

* RPM - 'nmot'
* Throttle plate angle - 'wdkba'
* Barometric Pressure - 'pus_w'
* Absolute Pressure - 'pvdks_w'

In ME7Tuner:

* Load a directory of logs using the **Load Logs** button
* Wait for ME7Tuner to finish the parse and calculations
* KFVPDKSD will be produced on the right side

---

## WDKUGDN (Throttle Body Choke Point)

WDKUGDN is a 1D Kennlinie (`RPM → throttle angle °`) that defines the **choked flow point** of the throttle body at each RPM. It tells the ECU at what throttle angle airflow transitions from **throttled** (restricted by the plate) to **unthrottled** (the throttle body is no longer the bottleneck — airflow is limited only by the turbo and engine).

This is a physical property of the throttle body. Not a tuning parameter. Not an alpha-n adjustment. Not something you change because a forum post told you to.

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

This keeps coming up, so let's be thorough about *why* it's wrong, not just *that* it's wrong.

WDKUGDN defines *when* the throttle chokes — it does **not** define *how much air flows* at a given angle. The common tuning advice ("compare msdk_w vs mshfm_w, adjust WDKUGDN until they match") conflates two completely different problems. It's like adjusting your speedometer cable because your fuel gauge reads wrong — you're turning the correct knob on the wrong instrument.

| Problem | Correct Solution | **Wrong** Solution |
|---------|------------------|--------------------|
| msdk_w ≠ mshfm_w (alpha-n inaccuracy) | Calibrate BGSRM VE model (KFURL/KFPBRK/KFPRG), KFMSNWDK | Adjusting WDKUGDN |
| Throttle body physically changed | Recalculate WDKUGDN for new bore diameter | Adjusting KFMSNWDK |

When you change WDKUGDN to "fix" an msdk_w vs mshfm_w discrepancy, here is exactly what goes wrong:

1. **The B_ugds flag fires at the wrong angle.** The ECU now thinks the throttle is choked when it isn't (or vice versa). Every downstream system that checks B_ugds — throttle control mode, fuel adaptation, boost control handoff — makes the wrong decision.

2. **Fuel adaptation (FUEREG) disables at the wrong throttle angle.** If WDKUGDN is set too low, FUEREG turns off too early and you lose closed-loop fueling in a region where the MAF is still the primary load signal. If it's set too high, FUEREG stays active into boost where it has no business being.

3. **The Überweg calculation produces wrong values.** The bypass interpolation between WDKUGDN and 100% throttle is calculated from the WDKUGDN breakpoint. Move the breakpoint and the entire pressure-ratio transition zone (KFVPDKSD region >0.95) is wrong.

4. **The actual problem (msdk_w ≠ mshfm_w) is still there.** You've just masked where it's visible by changing *when* the ECU switches between load calculation modes. The VE model is still wrong, the throttle-to-airflow mapping is still wrong, and the moment conditions change slightly, the error reappears in a new and more confusing way.

The correct fix is always to calibrate the maps that actually model airflow — KFMSNWDK for the throttle body flow curve, and the BGSRM VE maps (KFURL/KFPBRK/KFPRG) for the volumetric efficiency model. See [Alpha-N Calibration](#alpha-n-calibration--diagnostic-tool) below.

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

* Calculate WDKUGDN for your engine's displacement

---

## Alpha-N Calibration & Diagnostic Tool

Alpha-N (speed-density) mode is what happens when the MAF gives up — or more precisely, when the ECU determines the MAF has failed (`B_ehfm = true`) and switches from `mshfm_w` (MAF-measured airflow) to `msdk_w` (throttle-model-estimated airflow) as the sole load input. This also happens when you intentionally unplug the MAF, which some people do for racing applications because a broken MAF sensor can't report wrong values if it's not plugged in.

The catch: if `msdk_w` doesn't match reality when the MAF goes away, *everything* goes wrong — fueling, ignition timing, boost targets. The ECU's entire load model is built on an airflow signal that's now coming from a backup system that may never have been calibrated for your hardware.

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

The stock ME7 boost controller is a PID loop that fights the wastegate. It works, but it's reactive — it waits for boost error and then corrects. A feed-forward (pre-control) factor tells the PID *approximately* what duty cycle to start with at each RPM and boost level, so the PID only has to handle small corrections instead of doing all the work from scratch. The result is faster spool, less overshoot, and more consistent boost curves.

This is one of the highest-value calibrations you can do. Highly recommended for any setup — stock or modified.

The linearization process is tedious to do by hand — you need WOT pulls at various fixed duty cycles, manual duty cycle vs. boost pressure plotting, and careful interpolation. ME7Tuner automates most of this. You provide the logs; it provides the linearization table.

Read [Actual pre-control in LDRPID](http://nefariousmotorsports.com/forum/index.php?topic=12352.0title=)

<img src="/documentation/images/ldrpid.png" width="800">

### Algorithm

The algorithm is mostly based on [elRey's algorithm](http://nefariousmotorsports.com/forum/index.php?;topic=517.0) from the Nefarious Motorsports forums — one of the best-documented boost control linearization methods for ME7. The key difference: instead of using manual increments to build the linearization table (the way you'd do it by hand with a handful of data points), ME7Tuner fits a one-dimensional polynomial to the duty cycle vs. boost relationship. This produces a smoother result from far more data — ME7Tuner can parse millions of data points across dozens of log files, versus the handful of points you'd get from doing it manually.

### Usage

Log the following parameters:
* RPM — nmot
* Actual Absolute Manifold Pressure — pvdks_w
* Barometric Pressure — pus_w
* Throttle Plate Angle — wdkba
* Wastegate Duty Cycle — ldtvm
* Selected Gear — gangi

Do as many WOT pulls as possible, starting from the lowest RPM you can manage to the highest. You want a mix of pulls at fixed N75 duty cycles (for building the base linearization curve) and "real world" duty cycles (for validating the result). Different gears, different conditions — more data, better fit.

Put all of your logs in a single directory and select it in ME7Tuner with "Load ME7 Logs."

Then wait. Parsing thousands of WOT data points and fitting the polynomial takes time — especially if you've been thorough about logging. This is one of those "go make coffee" moments.

The linearized duty cycle will be output in KFLDRL. It may not be perfect out of the box — boost control linearization is sensitive to exhaust housing characteristics, wastegate spring rates, and turbo-to-turbo variation. Some manual adjustment is usually needed to get the final result dialed in.

For feed-forward pre-control, ME7Tuner will also output a new KFLDIMX and x-axis based on estimations from the linearized boost table. This is a ballpark — a starting point for the feed-forward map, not a finished product. Expect to iterate.

One practical tip: at RPM ranges where the turbo can't produce enough boost to crack the wastegates, request 95% duty cycle. There's no reason to be conservative when you're below the wastegate cracking pressure — you want the turbo spooling as hard as possible.
