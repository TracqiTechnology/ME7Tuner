package domain.model.optimizer

import data.contract.Me7LogFileContract
import domain.math.Index
import domain.math.map.Map3d
import domain.model.simulator.Me7Simulator
import domain.model.simulator.MechanicalLimitDetector
import domain.model.simulator.PidSimulator
import domain.model.simulator.SafetyModeDetector
import domain.model.simulator.TransientDetector
import domain.model.simulator.EnvironmentalCorrector
import domain.model.simulator.ThrottleBodyChecker
import kotlin.math.abs

/**
 * Suggestion engine that analyzes WOT logs and recommends corrections to
 * all maps in the LDRXN → actual_load signal chain.
 *
 * Integrates the ME7 simulation engine to predict ECU behavior from
 * calibration maps and compare against actual log data.
 *
 * @see domain.model.simulator.Me7Simulator
 * @see documentation/TECHNICAL_BREAKDOWN.md
 */
object OptimizerCalculator {

    /** Conversion factor: 1 mbar = 0.0145038 PSI */
    private const val MBAR_TO_PSI = 0.0145038

    /** Single row of relevant WOT data extracted from a log. */
    data class WotLogEntry(
        val rpm: Double,
        val requestedLoad: Double,      // rlsol_w
        val actualLoad: Double,         // rl_w (or rl)
        val requestedMap: Double,       // pssol_w (mbar absolute)
        val actualMap: Double,          // pvdks_w (mbar absolute)
        val barometricPressure: Double, // pus_w (mbar)
        val wgdc: Double,               // ldtvm (%)
        val throttleAngle: Double       // wdkba
    ) {
        /** Actual relative boost pressure in PSI (above atmospheric). */
        val relativeBoostPsi: Double get() = (actualMap - barometricPressure) * MBAR_TO_PSI
    }

    /** Complete result bundle from the optimizer. */
    data class OptimizerResult(
        val suggestedKfldrl: Map3d?,
        val suggestedKfldimx: Map3d?,
        val kfpbrkMultipliers: Map3d?,
        val pressureErrors: List<Pair<Double, Double>>,
        val loadErrors: List<Pair<Double, Double>>,
        val warnings: List<String>,
        val wotEntries: List<WotLogEntry>,
        // Simulation engine results
        val simulationResults: List<Me7Simulator.SimulationResult> = emptyList(),
        val mechanicalLimits: MechanicalLimitDetector.MechanicalLimits = MechanicalLimitDetector.MechanicalLimits(),
        val simulatedPressureSeries: List<Pair<Double, Double>> = emptyList(),
        val simulatedLoadSeries: List<Pair<Double, Double>> = emptyList(),
        // Chain diagnosis (v2)
        val chainDiagnosis: ChainDiagnosis = ChainDiagnosis(),
        // v3: Full auto-calibration output
        val suggestedMaps: SuggestedMaps = SuggestedMaps(),
        val perRpmAnalysis: Map<String, List<RpmBreakpointAnalysis>> = emptyMap(),
        val prediction: PredictionResult? = null,
        val logSummaries: List<LogSummary> = emptyList(),
        // v4: Advanced analysis
        val pulls: List<PullSegmenter.WotPull> = emptyList(),
        val pullConsistency: Map<Int, String> = emptyMap(),
        val safetyModes: SafetyModeDetector.SafetyModeResult? = null,
        val transients: TransientDetector.TransientResult? = null,
        val environmental: EnvironmentalCorrector.EnvironmentalSummary? = null,
        val throttleCheck: ThrottleBodyChecker.ThrottleCheckResult? = null,
        val convergenceHistory: IterativeConvergence.ConvergenceHistory? = null,
        val kfurlSolverResult: KfurlSolver.SolverResult? = null,
        val pidSimulation: PidSimulator.PidSimulationResult? = null
    )

    // ── Helpers ────────────────────────────────────────────────────────

    data class FilteredLogData(
        val wotEntries: List<WotLogEntry>,
        val mafValues: List<Double>?,
        val injectorOnTimes: List<Double>?,
        val wotRpms: List<Double>?
    )

    fun filterWotEntriesWithOptionalData(
        values: Map<Me7LogFileContract.Header, List<Double>>,
        minThrottleAngle: Double = 80.0
    ): FilteredLogData {
        val rpms = values[Me7LogFileContract.Header.RPM_COLUMN_HEADER] ?: return FilteredLogData(emptyList(), null, null, null)
        val requestedLoads = values[Me7LogFileContract.Header.REQUESTED_LOAD_HEADER] ?: return FilteredLogData(emptyList(), null, null, null)
        val actualLoads = values[Me7LogFileContract.Header.ENGINE_LOAD_HEADER] ?: return FilteredLogData(emptyList(), null, null, null)
        val requestedMaps = values[Me7LogFileContract.Header.REQUESTED_PRESSURE_HEADER] ?: return FilteredLogData(emptyList(), null, null, null)
        val actualMaps = values[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER] ?: return FilteredLogData(emptyList(), null, null, null)
        val wgdcs = values[Me7LogFileContract.Header.WASTEGATE_DUTY_CYCLE_HEADER] ?: return FilteredLogData(emptyList(), null, null, null)
        val throttleAngles = values[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER] ?: return FilteredLogData(emptyList(), null, null, null)
        val baroPressures = values[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER] ?: return FilteredLogData(emptyList(), null, null, null)

        val altLoads = values[Me7LogFileContract.Header.ACTUAL_LOAD_HEADER]
        val useAltLoads = altLoads != null && altLoads.size == rpms.size

        val allMafValues = values[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER]
        val hasMaf = allMafValues != null && allMafValues.size == rpms.size
        val allInjectorTimes = values[Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER]
        val hasInjector = allInjectorTimes != null && allInjectorTimes.size == rpms.size

        val entries = mutableListOf<WotLogEntry>()
        val wotMaf = if (hasMaf) mutableListOf<Double>() else null
        val wotInjector = if (hasInjector) mutableListOf<Double>() else null
        val wotRpms = if (hasInjector) mutableListOf<Double>() else null

        for (i in rpms.indices) {
            if (throttleAngles[i] >= minThrottleAngle) {
                entries.add(
                    WotLogEntry(
                        rpm = rpms[i],
                        requestedLoad = requestedLoads[i],
                        actualLoad = if (useAltLoads) altLoads!![i] else actualLoads[i],
                        requestedMap = requestedMaps[i],
                        actualMap = actualMaps[i],
                        barometricPressure = baroPressures[i],
                        wgdc = wgdcs[i],
                        throttleAngle = throttleAngles[i]
                    )
                )
                wotMaf?.add(allMafValues!![i])
                wotInjector?.add(allInjectorTimes!![i])
                wotRpms?.add(rpms[i])
            }
        }

        return FilteredLogData(entries, wotMaf, wotInjector, wotRpms)
    }

    fun filterWotEntries(
        values: Map<Me7LogFileContract.Header, List<Double>>,
        minThrottleAngle: Double = 80.0
    ): List<WotLogEntry> {
        return filterWotEntriesWithOptionalData(values, minThrottleAngle).wotEntries
    }

    // ── Phase 1 : Boost Control (KFLDRL + KFLDIMX) with MapDelta ──────

    /**
     * Suggest KFLDRL corrections and return a MapDelta with per-cell confidence.
     */
    fun suggestKfldrlDelta(
        wotEntries: List<WotLogEntry>,
        kfldrlMap: Map3d,
        toleranceMbar: Double = 30.0
    ): MapDelta {
        val rpmAxis = kfldrlMap.yAxis
        val pressureAxis = kfldrlMap.xAxis
        val suggested = Array(rpmAxis.size) { Array(pressureAxis.size) { 0.0 } }
        val sampleCounts = Array(rpmAxis.size) { IntArray(pressureAxis.size) }

        val wgdcSum = Array(rpmAxis.size) { DoubleArray(pressureAxis.size) }

        val boostEntries = wotEntries.filter { it.relativeBoostPsi > 0 }

        for (entry in boostEntries) {
            val rpmIdx = Index.getInsertIndex(rpmAxis.toList(), entry.rpm)
            val pressureIdx = Index.getInsertIndex(pressureAxis.toList(), entry.relativeBoostPsi)

            wgdcSum[rpmIdx][pressureIdx] += entry.wgdc
            sampleCounts[rpmIdx][pressureIdx]++
        }

        for (rpmIdx in rpmAxis.indices) {
            for (pIdx in pressureAxis.indices) {
                if (sampleCounts[rpmIdx][pIdx] > 0) {
                    suggested[rpmIdx][pIdx] = wgdcSum[rpmIdx][pIdx] / sampleCounts[rpmIdx][pIdx]
                } else {
                    suggested[rpmIdx][pIdx] = kfldrlMap.zAxis[rpmIdx][pIdx]
                }
            }
            // Enforce monotonicity
            for (pIdx in 1 until pressureAxis.size) {
                if (suggested[rpmIdx][pIdx] < suggested[rpmIdx][pIdx - 1] && sampleCounts[rpmIdx][pIdx] == 0) {
                    suggested[rpmIdx][pIdx] = suggested[rpmIdx][pIdx - 1]
                }
            }
        }

        val suggestedMap = Map3d(pressureAxis, rpmAxis, suggested)
        return MapDelta.build("KFLDRL", kfldrlMap, suggestedMap, sampleCounts)
    }

    /**
     * Legacy suggestKfldrl that returns just the Map3d.
     */
    fun suggestKfldrl(
        wotEntries: List<WotLogEntry>,
        kfldrlMap: Map3d,
        toleranceMbar: Double = 30.0,
        rpmTolerance: Double = 200.0
    ): Map3d {
        return suggestKfldrlDelta(wotEntries, kfldrlMap, toleranceMbar).suggested
    }

    /**
     * Suggest KFLDIMX as KFLDRL + overhead, returning a MapDelta.
     */
    fun suggestKfldimxDelta(
        suggestedKfldrlDelta: MapDelta,
        kfldimxMap: Map3d,
        overheadPercent: Double = 8.0
    ): MapDelta {
        val suggestedKfldrl = suggestedKfldrlDelta.suggested
        val multiplier = 1.0 + overheadPercent / 100.0

        val sampleCounts = Array(kfldimxMap.yAxis.size) { IntArray(kfldimxMap.xAxis.size) }

        val suggestedZ: Array<Array<Double>>
        if (kfldimxMap.yAxis.size == suggestedKfldrl.yAxis.size &&
            kfldimxMap.xAxis.size == suggestedKfldrl.xAxis.size
        ) {
            suggestedZ = Array(kfldimxMap.yAxis.size) { rpmIdx ->
                Array(kfldimxMap.xAxis.size) { pIdx ->
                    sampleCounts[rpmIdx][pIdx] = suggestedKfldrlDelta.sampleCounts[rpmIdx][pIdx]
                    val v = suggestedKfldrl.zAxis[rpmIdx][pIdx]
                    if (v > 0) v * multiplier else kfldimxMap.zAxis[rpmIdx][pIdx]
                }
            }
        } else {
            suggestedZ = Array(kfldimxMap.yAxis.size) { rpmIdx ->
                val kfldrlRpmIdx = Index.getInsertIndex(suggestedKfldrl.yAxis.toList(), kfldimxMap.yAxis[rpmIdx])
                Array(kfldimxMap.xAxis.size) { pIdx ->
                    val pressurePsi = kfldimxMap.xAxis[pIdx] * MBAR_TO_PSI
                    val kfldrlPIdx = Index.getInsertIndex(suggestedKfldrl.xAxis.toList(), pressurePsi)
                    if (kfldrlRpmIdx < suggestedKfldrlDelta.sampleCounts.size &&
                        kfldrlPIdx < suggestedKfldrlDelta.sampleCounts[0].size) {
                        sampleCounts[rpmIdx][pIdx] = suggestedKfldrlDelta.sampleCounts[kfldrlRpmIdx][kfldrlPIdx]
                    }
                    val v = suggestedKfldrl.zAxis[kfldrlRpmIdx][kfldrlPIdx]
                    if (v > 0) v * multiplier else kfldimxMap.zAxis[rpmIdx][pIdx]
                }
            }
        }

        val suggestedMap = Map3d(kfldimxMap.xAxis, kfldimxMap.yAxis, suggestedZ)
        return MapDelta.build("KFLDIMX", kfldimxMap, suggestedMap, sampleCounts)
    }

    /**
     * Legacy suggestKfldimx that returns just the Map3d.
     */
    fun suggestKfldimx(
        suggestedKfldrl: Map3d,
        kfldimxMap: Map3d,
        overheadPercent: Double = 8.0
    ): Map3d {
        val multiplier = 1.0 + overheadPercent / 100.0
        if (kfldimxMap.yAxis.size == suggestedKfldrl.yAxis.size &&
            kfldimxMap.xAxis.size == suggestedKfldrl.xAxis.size
        ) {
            val suggested = Array(kfldimxMap.yAxis.size) { rpmIdx ->
                Array(kfldimxMap.xAxis.size) { pIdx ->
                    val v = suggestedKfldrl.zAxis[rpmIdx][pIdx]
                    if (v > 0) v * multiplier else kfldimxMap.zAxis[rpmIdx][pIdx]
                }
            }
            return Map3d(kfldimxMap.xAxis, kfldimxMap.yAxis, suggested)
        }
        val suggested = Array(kfldimxMap.yAxis.size) { rpmIdx ->
            val kfldrlRpmIdx = Index.getInsertIndex(suggestedKfldrl.yAxis.toList(), kfldimxMap.yAxis[rpmIdx])
            Array(kfldimxMap.xAxis.size) { pIdx ->
                val pressurePsi = kfldimxMap.xAxis[pIdx] * MBAR_TO_PSI
                val kfldrlPIdx = Index.getInsertIndex(suggestedKfldrl.xAxis.toList(), pressurePsi)
                val v = suggestedKfldrl.zAxis[kfldrlRpmIdx][kfldrlPIdx]
                if (v > 0) v * multiplier else kfldimxMap.zAxis[rpmIdx][pIdx]
            }
        }
        return Map3d(kfldimxMap.xAxis, kfldimxMap.yAxis, suggested)
    }

    // ── Phase 2 : VE Model (KFPBRK) with MapDelta ─────────────────────

    /**
     * Suggest KFPBRK corrections and return a MapDelta with per-cell confidence.
     */
    fun suggestKfpbrkDelta(
        wotEntries: List<WotLogEntry>,
        kfpbrkMap: Map3d,
        toleranceMbar: Double = 30.0,
        rpmTolerance: Double = 200.0,
        simulationResults: List<Me7Simulator.SimulationResult>? = null
    ): MapDelta? {
        val rpmAxis = kfpbrkMap.yAxis
        val xAxis = kfpbrkMap.xAxis
        val suggested = Array(rpmAxis.size) { Array(xAxis.size) { 0.0 } }
        val sampleCounts = Array(rpmAxis.size) { IntArray(xAxis.size) }
        var hasData = false

        for (rpmIdx in rpmAxis.indices) {
            val rpmTarget = rpmAxis[rpmIdx]

            if (simulationResults != null) {
                val simValid = simulationResults.filter { sr ->
                    abs(sr.rpm - rpmTarget) < rpmTolerance &&
                        abs(sr.actualPvdks - sr.actualPssol) <= toleranceMbar &&
                        sr.actualRl > 0 &&
                        sr.kfpbrkCorrectionFactor.isFinite() &&
                        sr.kfpbrkCorrectionFactor > 0
                }

                if (simValid.isNotEmpty()) {
                    hasData = true
                    val avgCorrection = simValid.map { it.kfpbrkCorrectionFactor }.average()
                    val safeCorrection = if (avgCorrection.isFinite()) avgCorrection else 1.0
                    for (xIdx in xAxis.indices) {
                        suggested[rpmIdx][xIdx] = kfpbrkMap.zAxis[rpmIdx][xIdx] * safeCorrection
                        sampleCounts[rpmIdx][xIdx] = simValid.size
                    }
                    continue
                }
            }

            val valid = wotEntries.filter { e ->
                abs(e.rpm - rpmTarget) < rpmTolerance &&
                    abs(e.actualMap - e.requestedMap) <= toleranceMbar &&
                    e.actualLoad > 0
            }

            if (valid.isEmpty()) {
                for (xIdx in xAxis.indices) {
                    suggested[rpmIdx][xIdx] = kfpbrkMap.zAxis[rpmIdx][xIdx]
                }
                continue
            }

            hasData = true
            val avgRatio = valid.map { e -> e.requestedLoad / e.actualLoad }.average()
            val safeRatio = if (avgRatio.isFinite()) avgRatio else 1.0

            for (xIdx in xAxis.indices) {
                suggested[rpmIdx][xIdx] = kfpbrkMap.zAxis[rpmIdx][xIdx] * safeRatio
                sampleCounts[rpmIdx][xIdx] = valid.size
            }
        }

        if (!hasData) return null
        val suggestedMap = Map3d(xAxis, rpmAxis, suggested)
        return MapDelta.build("KFPBRK", kfpbrkMap, suggestedMap, sampleCounts)
    }

    /**
     * Legacy suggestKfpbrk that returns just the Map3d.
     */
    fun suggestKfpbrk(
        wotEntries: List<WotLogEntry>,
        kfpbrkMap: Map3d,
        toleranceMbar: Double = 30.0,
        rpmTolerance: Double = 200.0,
        simulationResults: List<Me7Simulator.SimulationResult>? = null
    ): Map3d? {
        return suggestKfpbrkDelta(wotEntries, kfpbrkMap, toleranceMbar, rpmTolerance, simulationResults)?.suggested
    }

    // ── Phase 2b : KFMIOP/KFMIRL Suggestions (Link 1 Fix) ──────────────

    /**
     * Suggest KFMIOP corrections for RPMs where the torque structure caps rlsol.
     *
     * KFMIOP maps (load%, RPM) → torque. If at a given RPM rlsol < LDRXN * 0.95,
     * the torque structure is limiting load. We scale the maximum torque output at
     * that RPM so that the load request can reach LDRXN.
     *
     * Algorithm: for each RPM row, find the peak torque value. If samples at that
     * RPM are torque-limited, compute the ratio (LDRXN / avg_rlsol) and scale
     * all torque cells in that row proportionally.
     */
    fun suggestKfmiopDelta(
        simulationResults: List<Me7Simulator.SimulationResult>,
        kfmiopMap: Map3d,
        ldrxnTarget: Double,
        rpmTolerance: Double = 250.0
    ): MapDelta? {
        val rpmAxis = kfmiopMap.yAxis
        val xAxis = kfmiopMap.xAxis
        val suggested = Array(rpmAxis.size) { r -> Array(xAxis.size) { c -> kfmiopMap.zAxis[r][c] } }
        val sampleCounts = Array(rpmAxis.size) { IntArray(xAxis.size) }
        var hasChanges = false

        for (rpmIdx in rpmAxis.indices) {
            val rpmTarget = rpmAxis[rpmIdx]
            val nearBy = simulationResults.filter { abs(it.rpm - rpmTarget) < rpmTolerance }
            if (nearBy.isEmpty()) continue

            val capped = nearBy.filter { it.torqueLimited }
            if (capped.isEmpty()) {
                // Not torque-limited at this RPM — keep original values
                for (xIdx in xAxis.indices) {
                    sampleCounts[rpmIdx][xIdx] = nearBy.size
                }
                continue
            }

            // Compute scale factor: how much we need to expand the torque range
            val avgRlsol = capped.map { it.rlsol }.average()
            if (avgRlsol <= 0) continue

            // Scale so the peak torque supports LDRXN instead of just avgRlsol
            val scaleFactor = ldrxnTarget / avgRlsol
            // Safety cap: don't scale more than 30%
            val safeFactor = scaleFactor.coerceAtMost(1.30)

            if (safeFactor > 1.005) {
                hasChanges = true
                for (xIdx in xAxis.indices) {
                    suggested[rpmIdx][xIdx] = kfmiopMap.zAxis[rpmIdx][xIdx] * safeFactor
                    sampleCounts[rpmIdx][xIdx] = capped.size
                }
            } else {
                for (xIdx in xAxis.indices) {
                    sampleCounts[rpmIdx][xIdx] = nearBy.size
                }
            }
        }

        if (!hasChanges) return null
        val suggestedMap = Map3d(xAxis, rpmAxis, suggested)
        return MapDelta.build("KFMIOP", kfmiopMap, suggestedMap, sampleCounts)
    }

    /**
     * Suggest KFMIRL as the inverse of the suggested KFMIOP.
     *
     * KFMIRL maps (torque, RPM) → load%. It is the mathematical inverse of KFMIOP.
     * We use the existing Inverse.calculateInverse() utility.
     */
    fun suggestKfmirlDelta(
        suggestedKfmiop: Map3d,
        kfmirlMap: Map3d
    ): MapDelta {
        val inverse = domain.math.Inverse.calculateInverse(suggestedKfmiop, kfmirlMap)
        // Preserve the first column from the original KFMIRL (idle/low-load values)
        for (i in inverse.zAxis.indices) {
            if (inverse.zAxis[i].isNotEmpty() && kfmirlMap.zAxis[i].isNotEmpty()) {
                inverse.zAxis[i][0] = kfmirlMap.zAxis[i][0]
            }
        }
        // Sample counts: use uniform count since inverse is a mathematical operation
        val sampleCounts = Array(kfmirlMap.yAxis.size) { IntArray(kfmirlMap.xAxis.size) { 100 } }
        return MapDelta.build("KFMIRL", kfmirlMap, inverse, sampleCounts)
    }

    // ── Phase 3 : Intervention Check ──────────────────────────────────

    fun checkInterventions(
        wotEntries: List<WotLogEntry>,
        ldrxnTarget: Double
    ): List<String> {
        val warnings = mutableListOf<String>()
        if (wotEntries.isEmpty()) return warnings

        val capped = wotEntries.filter { e -> e.requestedLoad < ldrxnTarget * 0.95 }
        if (capped.isNotEmpty()) {
            val pct = capped.size.toDouble() / wotEntries.size * 100
            val avgRlsol = capped.map { it.requestedLoad }.average()
            warnings.add(
                "Torque Intervention Detected: rlsol is below LDRXN target (${ldrxnTarget.format()}) " +
                    "in ${String.format("%.1f", pct)}% of WOT samples (avg rlsol = ${avgRlsol.format()}). " +
                    "Check KFMIOP / KFMIZUFIL to ensure torque requests support this load."
            )
        }

        val boostMissing = wotEntries.filter { e -> e.actualMap < e.requestedMap - 50 }
        if (boostMissing.size > wotEntries.size * 0.5) {
            val avgError = boostMissing.map { it.requestedMap - it.actualMap }.average()
            warnings.add(
                "Boost Target Not Reached: Actual pressure is below requested pressure by an average of " +
                    "${avgError.format()} mbar in ${String.format("%.1f", boostMissing.size.toDouble() / wotEntries.size * 100)}% " +
                    "of WOT samples. This may indicate a mechanical limitation (turbo spool, wastegate, boost leak) " +
                    "or incorrect KFLDRL base duty cycle."
            )
        }

        return warnings
    }

    // ── Chart data ────────────────────────────────────────────────────

    fun computePressureErrors(wotEntries: List<WotLogEntry>): List<Pair<Double, Double>> {
        return wotEntries.map { e -> Pair(e.rpm, e.requestedMap - e.actualMap) }
    }

    fun computeLoadErrors(wotEntries: List<WotLogEntry>): List<Pair<Double, Double>> {
        return wotEntries.filter { it.actualLoad > 0 }
            .map { e -> Pair(e.rpm, e.requestedLoad / e.actualLoad) }
    }

    // ── Per-RPM Breakpoint Analysis ───────────────────────────────────

    /**
     * Build per-RPM analysis for each chain link.
     */
    fun buildPerRpmAnalysis(
        simulationResults: List<Me7Simulator.SimulationResult>,
        rpmBreakpoints: Array<Double>?,
        rpmTolerance: Double = 250.0
    ): Map<String, List<RpmBreakpointAnalysis>> {
        if (simulationResults.isEmpty()) return emptyMap()

        // If no breakpoints provided, derive from data (500 RPM bins)
        val breakpoints = rpmBreakpoints ?: run {
            val minRpm = simulationResults.minOf { it.rpm }
            val maxRpm = simulationResults.maxOf { it.rpm }
            val step = 500.0
            val list = mutableListOf<Double>()
            var rpm = (minRpm / step).toInt() * step
            while (rpm <= maxRpm + step) {
                list.add(rpm)
                rpm += step
            }
            list.toTypedArray()
        }

        val link1 = mutableListOf<RpmBreakpointAnalysis>()
        val link2 = mutableListOf<RpmBreakpointAnalysis>()
        val link3 = mutableListOf<RpmBreakpointAnalysis>()
        val link4 = mutableListOf<RpmBreakpointAnalysis>()

        for (bp in breakpoints) {
            val nearBy = simulationResults.filter { abs(it.rpm - bp) < rpmTolerance }
            if (nearBy.isEmpty()) continue
            val count = nearBy.size
            val confidence = when {
                count < 5 -> MapDelta.Confidence.LOW
                count <= 20 -> MapDelta.Confidence.MEDIUM
                else -> MapDelta.Confidence.HIGH
            }

            // Link 1: torque headroom
            link1.add(RpmBreakpointAnalysis(
                rpm = bp,
                sampleCount = count,
                avgError = nearBy.map { it.torqueHeadroom }.average(),
                maxError = nearBy.maxOf { it.torqueHeadroom },
                correction = nearBy.count { it.torqueLimited }.toDouble() / count * 100,
                confidence = confidence
            ))

            // Link 2: pssol error
            link2.add(RpmBreakpointAnalysis(
                rpm = bp,
                sampleCount = count,
                avgError = nearBy.map { it.pssolError }.average(),
                maxError = nearBy.maxOf { abs(it.pssolError) },
                correction = nearBy.map { it.pssolError }.average(),
                confidence = confidence
            ))

            // Link 3: boost error
            link3.add(RpmBreakpointAnalysis(
                rpm = bp,
                sampleCount = count,
                avgError = nearBy.map { it.boostError }.average(),
                maxError = nearBy.maxOf { it.boostError },
                correction = nearBy.map { it.kfldrlCorrection }.average(),
                confidence = confidence
            ))

            // Link 4: VE mismatch
            link4.add(RpmBreakpointAnalysis(
                rpm = bp,
                sampleCount = count,
                avgError = nearBy.map { (it.kfpbrkCorrectionFactor - 1.0) * 100 }.average(),
                maxError = nearBy.maxOf { abs(it.kfpbrkCorrectionFactor - 1.0) * 100 },
                correction = nearBy.map { it.kfpbrkCorrectionFactor }.average(),
                confidence = confidence
            ))
        }

        return mapOf(
            "Link 1" to link1,
            "Link 2" to link2,
            "Link 3" to link3,
            "Link 4" to link4
        )
    }

    // ── Prediction Engine ─────────────────────────────────────────────

    /**
     * Predict what logs will look like after applying the suggested corrections.
     *
     * For each WOT entry:
     * - Estimate corrected pvdks from KFLDRL correction
     * - Estimate corrected rl_w from KFPBRK correction
     * - Re-run chain diagnosis on predicted values
     */
    fun predictOutcome(
        wotEntries: List<WotLogEntry>,
        simulationResults: List<Me7Simulator.SimulationResult>,
        suggestedMaps: SuggestedMaps,
        calibration: Me7Simulator.CalibrationSet,
        kfurl: Double
    ): PredictionResult {
        if (wotEntries.isEmpty() || simulationResults.isEmpty()) {
            return PredictionResult(
                predictedPressureSeries = emptyList(),
                predictedLoadSeries = emptyList(),
                currentAvgLoadDeficit = 0.0,
                predictedAvgLoadDeficit = 0.0,
                currentAvgPressureError = 0.0,
                predictedAvgPressureError = 0.0,
                predictedChainHealth = ChainDiagnosis(),
                convergenceImprovement = 0.0
            )
        }

        val predictedPressure = mutableListOf<Pair<Double, Double>>()
        val predictedLoad = mutableListOf<Pair<Double, Double>>()
        val predictedResults = mutableListOf<Me7Simulator.SimulationResult>()

        for (i in simulationResults.indices) {
            val sim = simulationResults[i]
            val entry = wotEntries[i]

            // Estimate corrected pvdks:
            // If KFLDRL correction says we need +X% WGDC, and we're providing it,
            // assume the boost error gets proportionally reduced.
            // Conservative: reduce boost error by 80% (not 100% due to PID dynamics)
            val boostImprovement = if (suggestedMaps.kfldrl != null) 0.80 else 0.0
            val correctedBoostError = sim.boostError * (1.0 - boostImprovement)
            val predictedPvdks = sim.actualPssol - correctedBoostError

            // Estimate corrected rl_w:
            // If KFPBRK correction factor is X, after correction it should be ~1.0
            val veImprovement = if (suggestedMaps.kfpbrk != null) 0.85 else 0.0
            val correctedVeError = (sim.kfpbrkCorrectionFactor - 1.0) * (1.0 - veImprovement)
            val predictedRl = if (sim.simulatedRlFromPressure > 0) {
                // Compute what rl_w would be at the predicted pressure with corrected VE
                val op = Me7Simulator.OperatingPoint(rpm = entry.rpm, barometricPressure = entry.barometricPressure)
                val rlAtPredictedPressure = Me7Simulator.computeRlFromPressure(
                    pressure = predictedPvdks, op = op, kfurl = kfurl,
                    previousPressure = entry.barometricPressure
                )
                rlAtPredictedPressure * (1.0 + correctedVeError)
            } else sim.actualRl

            predictedPressure.add(Pair(entry.rpm, predictedPvdks))
            predictedLoad.add(Pair(entry.rpm, predictedRl))

            // Build a predicted simulation result for chain diagnosis
            val predTorqueLimited = entry.requestedLoad < calibration.ldrxn * 0.95
            val predPssolError = sim.pssolError // PLSOL model doesn't change
            val predBoostError = correctedBoostError
            val predKfpbrkCorr = 1.0 + correctedVeError

            predictedResults.add(sim.copy(
                actualPvdks = predictedPvdks,
                boostError = predBoostError,
                actualRl = predictedRl,
                kfpbrkCorrectionFactor = predKfpbrkCorr,
                dominantError = Me7Simulator.diagnoseChain(predTorqueLimited, predPssolError, predBoostError, predKfpbrkCorr),
                totalLoadDeficit = calibration.ldrxn - predictedRl
            ))
        }

        val currentAvgLoadDeficit = simulationResults.map { it.totalLoadDeficit }.average()
        val predictedAvgLoadDeficit = predictedResults.map { it.totalLoadDeficit }.average()
        val currentAvgPressureError = simulationResults.map { it.boostError }.average()
        val predictedAvgPressureError = predictedResults.map { it.boostError }.average()

        val predictedChainHealth = buildChainDiagnosis(predictedResults, calibration.ldrxn, kfurl)

        val currentTotalError = abs(currentAvgLoadDeficit) + abs(currentAvgPressureError) / 10.0
        val predictedTotalError = abs(predictedAvgLoadDeficit) + abs(predictedAvgPressureError) / 10.0
        val improvement = if (currentTotalError > 0) {
            (1.0 - predictedTotalError / currentTotalError) * 100.0
        } else 0.0

        return PredictionResult(
            predictedPressureSeries = predictedPressure,
            predictedLoadSeries = predictedLoad,
            currentAvgLoadDeficit = currentAvgLoadDeficit,
            predictedAvgLoadDeficit = predictedAvgLoadDeficit,
            currentAvgPressureError = currentAvgPressureError,
            predictedAvgPressureError = predictedAvgPressureError,
            predictedChainHealth = predictedChainHealth,
            convergenceImprovement = improvement.coerceIn(0.0, 100.0)
        )
    }

    // ── Chain Diagnosis ──────────────────────────────────────────────

    data class ChainDiagnosis(
        val torqueCappedPercent: Double = 0.0,
        val pssolErrorPercent: Double = 0.0,
        val boostShortfallPercent: Double = 0.0,
        val veMismatchPercent: Double = 0.0,
        val onTargetPercent: Double = 0.0,
        val dominantError: Me7Simulator.ErrorSource = Me7Simulator.ErrorSource.ON_TARGET,
        val avgTotalLoadDeficit: Double = 0.0,
        val recommendations: List<String> = emptyList()
    )

    fun buildChainDiagnosis(
        simulationResults: List<Me7Simulator.SimulationResult>,
        ldrxnTarget: Double,
        kfurl: Double
    ): ChainDiagnosis {
        if (simulationResults.isEmpty()) return ChainDiagnosis()

        val total = simulationResults.size.toDouble()

        val torqueCapped = simulationResults.count { it.dominantError == Me7Simulator.ErrorSource.TORQUE_CAPPED }
        val pssolWrong = simulationResults.count { it.dominantError == Me7Simulator.ErrorSource.PSSOL_WRONG }
        val boostShort = simulationResults.count { it.dominantError == Me7Simulator.ErrorSource.BOOST_SHORTFALL }
        val veMismatch = simulationResults.count { it.dominantError == Me7Simulator.ErrorSource.VE_MISMATCH }
        val onTarget = simulationResults.count { it.dominantError == Me7Simulator.ErrorSource.ON_TARGET }

        val torquePct = torqueCapped / total * 100
        val pssolPct = pssolWrong / total * 100
        val boostPct = boostShort / total * 100
        val vePct = veMismatch / total * 100
        val okPct = onTarget / total * 100

        val errorCounts = mapOf(
            Me7Simulator.ErrorSource.TORQUE_CAPPED to torqueCapped,
            Me7Simulator.ErrorSource.PSSOL_WRONG to pssolWrong,
            Me7Simulator.ErrorSource.BOOST_SHORTFALL to boostShort,
            Me7Simulator.ErrorSource.VE_MISMATCH to veMismatch
        )
        val dominant = if (onTarget == simulationResults.size) {
            Me7Simulator.ErrorSource.ON_TARGET
        } else {
            errorCounts.maxByOrNull { it.value }?.key ?: Me7Simulator.ErrorSource.ON_TARGET
        }

        val avgDeficit = simulationResults.map { it.totalLoadDeficit }.average()

        val recs = mutableListOf<String>()

        if (torquePct > 20) {
            val avgHeadroom = simulationResults.filter { it.torqueLimited }.map { it.torqueHeadroom }.average()
            recs.add(
                "⚠️ Torque structure is capping load in ${String.format("%.0f", torquePct)}% of WOT samples. " +
                    "rlsol averages ${String.format("%.1f", avgHeadroom)}% below LDRXN (${ldrxnTarget.format()}%). " +
                    "Increase KFMIOP/KFMIRL ranges to support the target load."
            )
        }

        if (boostPct > 10) {
            val avgBoostError = simulationResults.filter { it.boostError > 0 }.map { it.boostError }.average()
            recs.add(
                "⚡ Boost shortfall in ${String.format("%.0f", boostPct)}% of WOT samples. " +
                    "Actual pressure falls short of pssol by avg ${String.format("%.0f", avgBoostError)} mbar. " +
                    "Apply suggested KFLDRL corrections to increase base duty cycle."
            )
        }

        if (vePct > 10) {
            val avgVeError = simulationResults.filter { abs(it.kfpbrkCorrectionFactor - 1.0) > 0.01 }
                .map { (it.kfpbrkCorrectionFactor - 1.0) * 100 }.average()
            recs.add(
                "📊 VE model mismatch in ${String.format("%.0f", vePct)}% of WOT samples. " +
                    "KFPBRK is off by avg ${String.format("%+.1f", avgVeError)}%. " +
                    "Apply suggested KFPBRK corrections so the ECU correctly converts pressure to load."
            )
        }

        if (pssolPct > 10) {
            val avgPssolError = simulationResults.filter { abs(it.pssolError) > 10 }.map { it.pssolError }.average()
            recs.add(
                "🔧 PLSOL model predicts wrong pressure in ${String.format("%.0f", pssolPct)}% of WOT samples. " +
                    "Simulated pssol deviates from logged pssol by avg ${String.format("%+.0f", avgPssolError)} mbar. " +
                    "Check KFURL value (currently ${String.format("%.4f", kfurl)}) — it may not match your engine's VE."
            )
        }

        if (recs.isEmpty() && okPct > 80) {
            recs.add(
                "✅ Signal chain is healthy: ${String.format("%.0f", okPct)}% of WOT samples are on-target. " +
                    "LDRXN=${ldrxnTarget.format()}% is being achieved."
            )
        }

        return ChainDiagnosis(
            torqueCappedPercent = torquePct,
            pssolErrorPercent = pssolPct,
            boostShortfallPercent = boostPct,
            veMismatchPercent = vePct,
            onTargetPercent = okPct,
            dominantError = dominant,
            avgTotalLoadDeficit = avgDeficit,
            recommendations = recs
        )
    }

    // ── Top-level entry point ─────────────────────────────────────────

    fun analyze(
        values: Map<Me7LogFileContract.Header, List<Double>>,
        kfldrlMap: Map3d?,
        kfldimxMap: Map3d?,
        kfpbrkMap: Map3d?,
        kfmiopMap: Map3d? = null,
        kfmirlMap: Map3d? = null,
        ldrxnTarget: Double = 191.0,
        toleranceMbar: Double = 30.0,
        minThrottleAngle: Double = 80.0,
        kfldimxOverheadPercent: Double = 8.0,
        kfurl: Double = 0.106,
        logSummaries: List<LogSummary> = emptyList(),
        // v4: PID gain maps (optional)
        kfldrq0Map: Map3d? = null,
        kfldrq1Map: Map3d? = null,
        kfldrq2Map: Map3d? = null,
        ldrq0dy: Double = 1.0
    ): OptimizerResult {
        val filteredData = filterWotEntriesWithOptionalData(values, minThrottleAngle)
        val wotEntries = filteredData.wotEntries

        // ── Simulation ───────────────────────────────────────────
        val calibration = Me7Simulator.CalibrationSet(
            kfpbrk = kfpbrkMap,
            kfldrl = kfldrlMap,
            kfldimx = kfldimxMap,
            kfurl = kfurl,
            ldrxn = ldrxnTarget
        )

        val simulationResults = Me7Simulator.simulateAll(wotEntries, calibration)

        // ── Mechanical limit detection ───────────────────────────
        val mechanicalLimits = MechanicalLimitDetector.detect(
            wotEntries = wotEntries,
            mafValues = filteredData.mafValues,
            injectorOnTimes = filteredData.injectorOnTimes,
            rpms = filteredData.wotRpms
        )

        // ── v3: MapDelta-based suggestions ──────────────────────
        val kfldrlDelta = if (kfldrlMap != null) {
            suggestKfldrlDelta(wotEntries, kfldrlMap, toleranceMbar)
        } else null

        val kfldimxDelta = if (kfldrlDelta != null && kfldimxMap != null) {
            suggestKfldimxDelta(kfldrlDelta, kfldimxMap, kfldimxOverheadPercent)
        } else null

        val kfpbrkDelta = if (kfpbrkMap != null) {
            suggestKfpbrkDelta(wotEntries, kfpbrkMap, toleranceMbar, simulationResults = simulationResults)
        } else null

        // ── v3: KFMIOP/KFMIRL suggestions (Link 1) ──────────────
        val kfmiopDelta = if (kfmiopMap != null && simulationResults.any { it.torqueLimited }) {
            suggestKfmiopDelta(simulationResults, kfmiopMap, ldrxnTarget)
        } else null

        val kfmirlDelta = if (kfmiopDelta != null && kfmirlMap != null) {
            suggestKfmirlDelta(kfmiopDelta.suggested, kfmirlMap)
        } else null

        val suggestedMaps = SuggestedMaps(
            kfldrl = kfldrlDelta,
            kfldimx = kfldimxDelta,
            kfpbrk = kfpbrkDelta,
            kfmiop = kfmiopDelta,
            kfmirl = kfmirlDelta
        )

        // ── Legacy Map3d results (backward compat for existing tabs) ──
        val suggestedKfldrl = kfldrlDelta?.suggested
        val suggestedKfldimx = kfldimxDelta?.suggested
        val kfpbrkMultipliers = kfpbrkDelta?.suggested

        val pressureErrors = computePressureErrors(wotEntries)
        val loadErrors = computeLoadErrors(wotEntries)

        // ── Warnings ────────────────────────────────────────────
        val interventionWarnings = checkInterventions(wotEntries, ldrxnTarget)
        val allWarnings = interventionWarnings + mechanicalLimits.warnings

        // ── Simulation chart series ─────────────────────────────
        val simulatedPressureSeries = simulationResults.map { Pair(it.rpm, it.simulatedPssol) }
        val simulatedLoadSeries = simulationResults
            .filter { it.simulatedRlFromPressure > 0 }
            .map { Pair(it.rpm, it.simulatedRlFromPressure) }

        // ── Chain diagnosis ─────────────────────────────────────
        val chainDiagnosis = buildChainDiagnosis(simulationResults, ldrxnTarget, kfurl)

        // ── Per-RPM analysis ────────────────────────────────────
        val rpmBreakpoints = kfldrlMap?.yAxis
        val perRpmAnalysis = buildPerRpmAnalysis(simulationResults, rpmBreakpoints)

        // ── Prediction ──────────────────────────────────────────
        val prediction = if (wotEntries.isNotEmpty() && simulationResults.isNotEmpty()) {
            predictOutcome(wotEntries, simulationResults, suggestedMaps, calibration, kfurl)
        } else null

        // ── v4: Advanced analysis ─────────────────────────────
        // Phase 21: Per-pull segmentation
        val pulls = PullSegmenter.segmentPulls(wotEntries, ldrxnTarget = ldrxnTarget)
        val pullConsistency = PullSegmenter.checkConsistency(pulls)

        // Phase 17: Safety mode detection
        val safetyModes = SafetyModeDetector.detect(wotEntries)

        // Phase 16: Transient event detection
        val transients = TransientDetector.detect(wotEntries, ldrxnTarget)

        // Phase 18: Environmental conditions
        val environmental = EnvironmentalCorrector.analyzeSummary(wotEntries)

        // Phase 19: Throttle body check
        val throttleCheck = ThrottleBodyChecker.check(wotEntries, mechanicalLimits.turboMaxed)

        // Phase 22: KFURL solver
        val kfurlSolverResult = if (wotEntries.size >= 10) {
            KfurlSolver.solve(wotEntries)
        } else null

        // Phase 20: Iterative convergence (only if we have enough data and maps)
        val convergenceHistory = if (wotEntries.size >= 20 &&
            (kfldrlMap != null || kfpbrkMap != null)) {
            IterativeConvergence.converge(
                wotEntries = wotEntries,
                initialCalibration = calibration,
                kfldrlMap = kfldrlMap,
                kfldimxMap = kfldimxMap,
                kfpbrkMap = kfpbrkMap,
                kfmiopMap = kfmiopMap,
                kfmirlMap = kfmirlMap,
                ldrxnTarget = ldrxnTarget,
                toleranceMbar = toleranceMbar,
                kfldimxOverheadPercent = kfldimxOverheadPercent
            )
        } else null

        // Phase 15: PID dynamics simulation (on the longest good pull)
        val pidSimulation = if (kfldrq0Map != null || kfldrq1Map != null || kfldrq2Map != null) {
            val bestPull = pulls.filter { it.quality == PullSegmenter.PullQuality.GOOD }
                .maxByOrNull { it.sampleCount }
                ?: pulls.maxByOrNull { it.sampleCount }

            if (bestPull != null && bestPull.sampleCount >= 10) {
                val pullEntries = wotEntries.subList(
                    bestPull.startIdx,
                    (bestPull.endIdx + 1).coerceAtMost(wotEntries.size)
                )
                PidSimulator.simulate(
                    pullEntries = pullEntries,
                    kfldrq0 = kfldrq0Map,
                    kfldrq1 = kfldrq1Map,
                    kfldrq2 = kfldrq2Map,
                    kfldrl = kfldrlMap,
                    kfldimx = kfldimxMap,
                    ldrq0dy = ldrq0dy
                )
            } else null
        } else null

        // ── Collect v4 warnings ───────────────────────────────
        val v4Warnings = mutableListOf<String>()
        v4Warnings.addAll(safetyModes.warnings)
        v4Warnings.addAll(transients.warnings)
        v4Warnings.addAll(environmental.warnings)
        if (throttleCheck.restricted) {
            v4Warnings.add("⚠️ Throttle restriction: ${throttleCheck.detail}")
        }
        if (kfurlSolverResult != null && kfurlSolverResult.errorReductionPercent > 10) {
            v4Warnings.add("💡 KFURL solver suggests ${String.format("%.4f", kfurlSolverResult.optimalKfurl)} " +
                "(current: $kfurl) — would reduce pssol RMSE by ${String.format("%.0f", kfurlSolverResult.errorReductionPercent)}%")
        }
        if (convergenceHistory != null && convergenceHistory.diverged) {
            v4Warnings.add("⚠️ Iterative convergence diverged — corrections may be oscillating. Consider reducing LDRXN target.")
        }
        pullConsistency.forEach { (pullIdx, note) ->
            v4Warnings.add("🔍 Pull ${pullIdx + 1}: $note")
        }
        pidSimulation?.diagnosis?.recommendations?.forEach { rec ->
            v4Warnings.add("🎛️ PID: $rec")
        }

        val allWarningsV4 = allWarnings + v4Warnings

        return OptimizerResult(
            suggestedKfldrl = suggestedKfldrl,
            suggestedKfldimx = suggestedKfldimx,
            kfpbrkMultipliers = kfpbrkMultipliers,
            pressureErrors = pressureErrors,
            loadErrors = loadErrors,
            warnings = allWarningsV4,
            wotEntries = wotEntries,
            simulationResults = simulationResults,
            mechanicalLimits = mechanicalLimits,
            simulatedPressureSeries = simulatedPressureSeries,
            simulatedLoadSeries = simulatedLoadSeries,
            chainDiagnosis = chainDiagnosis,
            suggestedMaps = suggestedMaps,
            perRpmAnalysis = perRpmAnalysis,
            prediction = prediction,
            logSummaries = logSummaries,
            // v4
            pulls = pulls,
            pullConsistency = pullConsistency,
            safetyModes = safetyModes,
            transients = transients,
            environmental = environmental,
            throttleCheck = throttleCheck,
            convergenceHistory = convergenceHistory,
            kfurlSolverResult = kfurlSolverResult,
            pidSimulation = pidSimulation
        )
    }

    private fun Double.format(): String = String.format("%.2f", this)
}
