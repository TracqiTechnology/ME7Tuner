package domain.model.alphan

import data.contract.Me7LogFileContract
import domain.math.map.Map3d
import domain.model.rlsol.Rlsol
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Alpha-N (Speed-Density) Diagnostic Tool.
 *
 * Compares mshfm_w (MAF-measured airflow) against msdk_w (throttle-model-estimated
 * airflow) to assess alpha-n readiness and classify error sources.
 *
 * When manifold pressure (pvdks_w) and barometric pressure (pus_w) are available in
 * the log, also solves for optimal KFURL, KFPRG, and per-RPM KFPBRK correction
 * factors that would bring the VE model into alignment with reality.
 *
 * **Advantage over WOT-only Optimizer:** Alpha-n logs span idle → cruise → WOT, giving
 * better coverage of KFPRG (dominant at low load where residual gas fraction is highest)
 * and full-range KFURL characterization.
 *
 * **Background:** ME7 runs two parallel load paths. The MAF (mshfm_w) is the primary
 * signal; the throttle model (msdk_w) is the backup. When the MAF is unplugged or
 * fails, the ECU switches to msdk_w exclusively. If msdk_w is inaccurate, the car
 * runs poorly (wrong fueling, wrong timing, wrong boost).
 *
 * The ECU continuously adapts msdk_w to match mshfm_w via:
 * - msndko_w (additive offset for leak air) — me7-raw.txt line 52954
 * - fkmsdk_w (multiplicative factor) — me7-raw.txt line 52950
 *
 * When the MAF is unplugged, these adaptations freeze. They must be learned
 * correctly beforehand for alpha-n to work.
 *
 * @see documentation/me7-alpha-n-calibration.md
 */
object AlphaNDiagnostic {

    // ── Constants ─────────────────────────────────────────────────────

    /** Default KFURL value (%/hPa) — ME7 M-box typical. */
    private const val DEFAULT_KFURL = 0.106

    /** Default KFPRG value (hPa) — ME7 M-box typical. */
    private const val DEFAULT_KFPRG = 70.0

    /** Default KFPBRK value (dimensionless) — ME7 M-box typical. */
    private const val DEFAULT_KFPBRK = 1.016

    /**
     * KUMSRL for 2.7L 6-cylinder: converts mass flow (kg/h) to relative load (%).
     * rl = mshfm_w / (KUMSRL × nmot)
     * For a 2.7L V6, KUMSRL ≈ 0.003535 (derived from displacement/120/efficiency).
     */
    private const val DEFAULT_KUMSRL = 0.003535

    // ── Enums ─────────────────────────────────────────────────────────

    /** Severity rating for alpha-n readiness. */
    enum class Severity {
        /** msdk_w matches mshfm_w within 5% — alpha-n will run well */
        GOOD,
        /** msdk_w deviates 5-15% — alpha-n will run but may be rough */
        WARNING,
        /** msdk_w deviates >15% — alpha-n will run poorly or stall */
        CRITICAL
    }

    /** Classification of the dominant error type between mshfm_w and msdk_w. */
    enum class ErrorType {
        /** Error is roughly constant across all airflow values — additive offset (msndko_w) */
        ADDITIVE,
        /** Error scales proportionally with airflow — multiplicative factor (fkmsdk_w / KFMSNWDK) */
        MULTIPLICATIVE,
        /** Error varies with RPM but not with airflow — VE model error (KFURL/KFPBRK) */
        RPM_DEPENDENT,
        /** Mixed or unclassifiable error pattern */
        MIXED,
        /** Not enough data to classify */
        INSUFFICIENT_DATA
    }

    /** Confidence level for VE model suggestions. */
    enum class Confidence {
        /** < 5 samples per bin — use with extreme caution */
        LOW,
        /** 5–20 samples per bin — reasonable estimate, verify with WOT logs */
        MEDIUM,
        /** > 20 samples per bin — high confidence from dense data */
        HIGH
    }

    // ── Data classes ──────────────────────────────────────────────────

    /** Per-RPM bin analysis result. */
    data class RpmBinResult(
        val rpmCenter: Double,
        val sampleCount: Int,
        val avgMshfm: Double,
        val avgMsdk: Double,
        val avgErrorPercent: Double,
        val avgErrorAbsolute: Double,  // kg/h
        val maxErrorPercent: Double,
        val rmsErrorPercent: Double
    )

    /** Suggested KFURL correction with error metrics. */
    data class KfurlSuggestion(
        /** Best-fit scalar KFURL across all data */
        val optimalKfurl: Double,
        /** RMSE (% load) with default KFURL */
        val currentRmse: Double,
        /** RMSE (% load) with suggested KFURL */
        val suggestedRmse: Double,
        /** Percentage error reduction */
        val errorReductionPercent: Double,
        /** Per-RPM optimal KFURL values (RPM → KFURL), or null if insufficient data */
        val perRpmValues: List<Pair<Double, Double>>?
    )

    /** Suggested KFPRG correction with error metrics. */
    data class KfprgSuggestion(
        /** Best-fit scalar KFPRG (hPa) across all data */
        val optimalKfprg: Double,
        /** RMSE (% load) with default KFPRG */
        val currentRmse: Double,
        /** RMSE (% load) with suggested KFPRG */
        val suggestedRmse: Double,
        /** Percentage error reduction */
        val errorReductionPercent: Double,
        /** Per-RPM optimal KFPRG values (RPM → hPa), or null if insufficient data */
        val perRpmValues: List<Pair<Double, Double>>?
    )

    /** Per-RPM KFPBRK correction factor. */
    data class KfpbrkCorrection(
        val rpmCenter: Double,
        /** Multiply current KFPBRK by this factor at this RPM */
        val correctionFactor: Double,
        val sampleCount: Int,
        val confidence: Confidence
    )

    /** Complete alpha-n diagnostic result. */
    data class AlphaNResult(
        /** Overall severity rating */
        val severity: Severity,
        /** Dominant error type classification */
        val errorType: ErrorType,
        /** Overall average error (%) between mshfm_w and msdk_w */
        val avgErrorPercent: Double,
        /** Overall RMS error (%) */
        val rmsErrorPercent: Double,
        /** Maximum absolute error (%) observed */
        val maxErrorPercent: Double,
        /** Per-RPM bin breakdown */
        val rpmBins: List<RpmBinResult>,
        /** Total samples analyzed */
        val totalSamples: Int,
        /** Actionable recommendations */
        val recommendations: List<String>,
        /** Per-sample data (for charting): RPM, mshfm, msdk */
        val errorSeries: List<Triple<Double, Double, Double>>,
        /** Estimated multiplicative correction factor (if MULTIPLICATIVE error) */
        val estimatedMultiplicativeCorrection: Double?,
        /** Estimated additive correction in kg/h (if ADDITIVE error) */
        val estimatedAdditiveCorrection: Double?,
        // ── VE Model Suggestions (when pvdks_w + pus_w available) ──
        /** Suggested KFURL correction, or null if insufficient pressure data */
        val kfurlSuggestion: KfurlSuggestion? = null,
        /** Suggested KFPRG correction, or null if insufficient pressure data */
        val kfprgSuggestion: KfprgSuggestion? = null,
        /** Per-RPM KFPBRK correction factors, or null if insufficient data */
        val kfpbrkCorrections: List<KfpbrkCorrection>? = null,
        /** Whether pressure data was available for VE model solving */
        val hasPressureData: Boolean = false,
        // ── Map3d outputs for table display and BIN write ──
        /** Current KFURL Map3d read from BIN (input table) */
        val currentKfurlMap: Map3d? = null,
        /** Suggested KFURL Map3d with solver-corrected values (output table) */
        val suggestedKfurlMap: Map3d? = null,
        /** Current KFPBRK Map3d read from BIN (input table) */
        val currentKfpbrkMap: Map3d? = null,
        /** Suggested KFPBRK Map3d with correction factors applied (output table) */
        val suggestedKfpbrkMap: Map3d? = null
    )

    // ── Internal sample with pressure data ────────────────────────────

    private data class Sample(
        val rpm: Double,
        val mshfm: Double,
        val msdk: Double,
        val throttle: Double
    )

    private data class PressureSample(
        val rpm: Double,
        val mshfmLoad: Double,   // MAF-derived load (truth) — rl_w or g/s→% conversion
        val pvdks: Double,       // pre-throttle pressure (mbar) — pvdks_w = "Druck Vor DrosselKlappe Sensor"
        val pus: Double,         // barometric pressure (mbar)
        val wdkba: Double        // throttle plate angle (°) — needed for WOT filtering
    )

    // ── Main analyze() ────────────────────────────────────────────────

    /**
     * Analyze alpha-n accuracy from log data containing both mshfm_w and msdk_w.
     *
     * When pvdks_w and pus_w are also present, runs VE model solvers (KFURL, KFPRG,
     * KFPBRK) to suggest specific map corrections.
     *
     * @param logData Parsed log data map from AlphaNLogParser
     * @param minThrottleAngle Minimum throttle angle to include (default 5°)
     * @param rpmBinWidth RPM bin width for per-RPM analysis (default 500)
     * @param currentKfurl Current KFURL value for comparison (default 0.106)
     * @param currentKfprg Current KFPRG value for comparison (default 70.0 hPa)
     * @param kfpbrkMap Optional KFPBRK Map3d read from BIN for per-cell lookup
     * @param displacement Engine displacement in liters (default 2.7)
     * @param currentKfurlMap Optional KFURL Map3d read from BIN — used to build suggested output map with matching axes
     * @return AlphaNResult with diagnostics and VE suggestions, or null if required data missing
     */
    fun analyze(
        logData: Map<Me7LogFileContract.Header, List<Double>>,
        minThrottleAngle: Double = 5.0,
        rpmBinWidth: Double = 500.0,
        currentKfurl: Double = DEFAULT_KFURL,
        currentKfprg: Double = DEFAULT_KFPRG,
        kfpbrkMap: Map3d? = null,
        displacement: Double = 2.7,
        currentKfurlMap: Map3d? = null
    ): AlphaNResult? {
        val rpms = logData[Me7LogFileContract.Header.RPM_COLUMN_HEADER] ?: return null
        val mshfmValues = logData[Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER] ?: return null
        val msdkValues = logData[Me7LogFileContract.Header.THROTTLE_MODEL_AIRFLOW_HEADER] ?: return null
        val throttleAngles = logData[Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER]
        val pvdksValues = logData[Me7LogFileContract.Header.ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER]
        val pusValues = logData[Me7LogFileContract.Header.BAROMETRIC_PRESSURE_HEADER]

        if (rpms.size != mshfmValues.size || rpms.size != msdkValues.size) return null
        if (rpms.isEmpty()) return null

        // Filter: require minimum throttle angle if available, and mshfm > 1 kg/h to avoid div/0
        val samples = mutableListOf<Sample>()
        for (i in rpms.indices) {
            val throttle = throttleAngles?.getOrNull(i) ?: 90.0
            if (throttle >= minThrottleAngle && mshfmValues[i] > 1.0 && rpms[i] > 500) {
                samples.add(Sample(rpms[i], mshfmValues[i], msdkValues[i], throttle))
            }
        }

        if (samples.size < 10) {
            return AlphaNResult(
                severity = Severity.CRITICAL,
                errorType = ErrorType.INSUFFICIENT_DATA,
                avgErrorPercent = 0.0,
                rmsErrorPercent = 0.0,
                maxErrorPercent = 0.0,
                rpmBins = emptyList(),
                totalSamples = samples.size,
                recommendations = listOf(
                    "Insufficient data: need at least 10 samples with both mshfm_w and msdk_w logged.",
                    "Ensure your ME7Logger config includes both mshfm_w and msdk_w channels."
                ),
                errorSeries = emptyList(),
                estimatedMultiplicativeCorrection = null,
                estimatedAdditiveCorrection = null
            )
        }

        // Compute per-sample errors
        val errorPercents = samples.map { s ->
            if (s.mshfm > 1.0) (s.mshfm - s.msdk) / s.mshfm * 100.0 else 0.0
        }
        val errorAbsolutes = samples.map { s -> s.mshfm - s.msdk }

        val avgError = errorPercents.average()
        val rmsError = sqrt(errorPercents.map { it * it }.average())
        val maxError = errorPercents.maxOfOrNull { abs(it) } ?: 0.0

        // Error series for charting
        val errorSeries = samples.map { Triple(it.rpm, it.mshfm, it.msdk) }

        // Per-RPM bin analysis
        val minRpm = samples.minOf { it.rpm }
        val maxRpm = samples.maxOf { it.rpm }
        val rpmBins = mutableListOf<RpmBinResult>()

        var rpmCenter = (minRpm / rpmBinWidth).toInt() * rpmBinWidth + rpmBinWidth / 2
        while (rpmCenter <= maxRpm + rpmBinWidth / 2) {
            val binSamples = samples.filter { abs(it.rpm - rpmCenter) < rpmBinWidth / 2 }
            if (binSamples.isNotEmpty()) {
                val binErrors = binSamples.map { s ->
                    if (s.mshfm > 1.0) (s.mshfm - s.msdk) / s.mshfm * 100.0 else 0.0
                }
                val binAbsErrors = binSamples.map { s -> s.mshfm - s.msdk }
                rpmBins.add(
                    RpmBinResult(
                        rpmCenter = rpmCenter,
                        sampleCount = binSamples.size,
                        avgMshfm = binSamples.map { it.mshfm }.average(),
                        avgMsdk = binSamples.map { it.msdk }.average(),
                        avgErrorPercent = binErrors.average(),
                        avgErrorAbsolute = binAbsErrors.average(),
                        maxErrorPercent = binErrors.maxOfOrNull { abs(it) } ?: 0.0,
                        rmsErrorPercent = sqrt(binErrors.map { it * it }.average())
                    )
                )
            }
            rpmCenter += rpmBinWidth
        }

        // Classify error type
        val errorType = classifyErrorType(samples, rpmBins)

        // Estimate corrections
        val multiplicativeCorrection = if (errorType == ErrorType.MULTIPLICATIVE || errorType == ErrorType.MIXED) {
            val validSamples = samples.filter { it.mshfm > 5.0 }
            if (validSamples.isNotEmpty()) {
                validSamples.map { it.mshfm / it.msdk }.average()
            } else null
        } else null

        val additiveCorrection = if (errorType == ErrorType.ADDITIVE || errorType == ErrorType.MIXED) {
            errorAbsolutes.average()
        } else null

        // Severity
        val absAvgError = abs(avgError)
        val severity = when {
            absAvgError <= 5.0 && rmsError <= 7.0 -> Severity.GOOD
            absAvgError <= 15.0 && rmsError <= 20.0 -> Severity.WARNING
            else -> Severity.CRITICAL
        }

        // ── VE Model Solving (when pressure data available) ───────────
        val hasPressure = pvdksValues != null && pusValues != null &&
            pvdksValues.size == rpms.size && pusValues.size == rpms.size

        var kfurlSuggestion: KfurlSuggestion? = null
        var kfprgSuggestion: KfprgSuggestion? = null
        var kfpbrkCorrections: List<KfpbrkCorrection>? = null

        // Also check for rl_w (actual load in %) — preferred over KUMSRL conversion
        val rlValues = logData[Me7LogFileContract.Header.ENGINE_LOAD_HEADER]
        val hasRlValues = rlValues != null && rlValues.size == rpms.size

        // Auto-detect rl_w units: ME7Logger normally outputs % (34.875 at idle, 82.4 at cruise).
        // Some ECU variants or tools may output as fraction (0.34875 at idle, 0.824 at cruise).
        // If max(rl_w) < 3.0, it's a fraction and needs ×100 to become %.
        val rlScale = if (hasRlValues && rlValues != null && rlValues.max() < 3.0) 100.0 else 1.0

        if (hasPressure && severity != Severity.GOOD) {
            // Build pressure samples with load truth
            // Prefer rl_w (already in % load, directly from ECU) over KUMSRL g/s→% conversion
            val pressureSamples = mutableListOf<PressureSample>()
            for (i in rpms.indices) {
                val throttle = throttleAngles?.getOrNull(i) ?: 90.0
                if (throttle >= minThrottleAngle && mshfmValues[i] > 1.0 && rpms[i] > 500) {
                    val pvdks = pvdksValues!![i]
                    val pus = pusValues!![i]
                    if (pvdks > 100 && pus > 800) {
                        val load: Double = if (hasRlValues && rlValues!![i] > 0.5) {
                            // Use rl_w directly — already in % relative load (with auto-scaling)
                            rlValues[i] * rlScale
                        } else {
                            // Fallback: convert mshfm_w (g/s) → relative load (%)
                            //
                            // ME7 formula: rl = mshfm_internal / umsrln_w
                            //   where umsrln_w = KUMSRL × nmot
                            //
                            // ME7Logger reports mshfm_w in g/s. ME7 internal is mg/hub (per intake stroke).
                            // For a 4-stroke nCyl engine: strokes/sec = RPM × nCyl / 120
                            //   mg/hub = g/s × 1000 / strokes_per_sec
                            //
                            // 100% load = theoretical fill at STP:
                            //   theoreticalMgPerHub = (displacement_cc / nCyl) × airDensity_mg_per_cc
                            //   At STP: airDensity ≈ 1.189 kg/m³ = 1.189 mg/cc
                            //
                            // rl% = (mg/hub) / theoreticalMgPerHub × 100
                            val nCyl = if (displacement > 2.0) 6.0 else 4.0  // heuristic
                            val strokesPerSec = rpms[i] * nCyl / 120.0
                            val mshfmMgPerHub = mshfmValues[i] * 1000.0 / strokesPerSec
                            val displacementCc = displacement * 1000.0
                            val airDensityMgPerCc = 1.189  // mg/cc at STP (1.189 kg/m³)
                            val theoreticalMgPerHub = (displacementCc / nCyl) * airDensityMgPerCc
                            mshfmMgPerHub / theoreticalMgPerHub * 100.0
                        }
                        pressureSamples.add(PressureSample(rpms[i], load, pvdks, pus, throttle))
                    }
                }
            }

            if (pressureSamples.size >= 20) {
                val wotCount = pressureSamples.count { it.wdkba >= 80.0 }
                val maxPvdks = pressureSamples.maxOf { it.pvdks }
                val avgLoad = pressureSamples.map { it.mshfmLoad }.average()
                println("[AlphaN] VE solver: ${pressureSamples.size} total, $wotCount WOT(>=80°), pvdks max=${String.format("%.0f", maxPvdks)}, avgLoad=${String.format("%.1f", avgLoad)}%, rlScale=$rlScale, kfurl=$currentKfurl")

                kfurlSuggestion = solveKfurl(pressureSamples, currentKfurl, currentKfprg, kfpbrkMap, rpmBinWidth)
                val bestKfurl = kfurlSuggestion?.optimalKfurl ?: currentKfurl

                // KFPRG and KFPBRK also use Rlsol(pvdks), so they also need WOT filtering.
                // Use the same WOT-filtered subset for consistency.
                val wotForSolvers = pressureSamples.filter { it.wdkba >= 80.0 }
                val solverSamples = if (wotForSolvers.size >= 20) wotForSolvers else {
                    val highLoadFallback = pressureSamples.filter { it.mshfmLoad > 80.0 }
                    if (highLoadFallback.size >= 20) highLoadFallback else pressureSamples
                }

                kfprgSuggestion = solveKfprg(solverSamples, bestKfurl, currentKfprg, kfpbrkMap, rpmBinWidth)
                val bestKfprg = kfprgSuggestion?.optimalKfprg ?: currentKfprg
                kfpbrkCorrections = solveKfpbrk(solverSamples, bestKfurl, bestKfprg, kfpbrkMap, rpmBinWidth)
            }
        }

        // Recommendations (enhanced with VE suggestions)
        val recommendations = buildRecommendations(
            severity, errorType, avgError, rpmBins,
            multiplicativeCorrection, additiveCorrection,
            kfurlSuggestion, kfprgSuggestion, kfpbrkCorrections, hasPressure
        )

        // ── Build Map3d outputs for table display / BIN write ─────────
        val suggestedKfurlMap = buildSuggestedKfurlMap(currentKfurlMap, kfurlSuggestion)
        val suggestedKfpbrkMap = buildSuggestedKfpbrkMap(kfpbrkMap, kfpbrkCorrections)

        return AlphaNResult(
            severity = severity,
            errorType = errorType,
            avgErrorPercent = avgError,
            rmsErrorPercent = rmsError,
            maxErrorPercent = maxError,
            rpmBins = rpmBins,
            totalSamples = samples.size,
            recommendations = recommendations,
            errorSeries = errorSeries,
            estimatedMultiplicativeCorrection = multiplicativeCorrection,
            estimatedAdditiveCorrection = additiveCorrection,
            kfurlSuggestion = kfurlSuggestion,
            kfprgSuggestion = kfprgSuggestion,
            kfpbrkCorrections = kfpbrkCorrections,
            hasPressureData = hasPressure,
            currentKfurlMap = currentKfurlMap,
            suggestedKfurlMap = suggestedKfurlMap,
            currentKfpbrkMap = kfpbrkMap,
            suggestedKfpbrkMap = suggestedKfpbrkMap
        )
    }

    // ── VE Model Solvers ──────────────────────────────────────────────

    /**
     * Compute KUMSRL from displacement.
     * KUMSRL converts (kg/h) / RPM → relative load (%).
     * Formula: KUMSRL = displacement_liters / (120 × volumetric_efficiency_factor)
     * Using a typical VE factor of ~0.637 for a 4-stroke engine.
     */
    private fun computeKumsrl(displacementLiters: Double): Double {
        return displacementLiters / (120.0 * 0.637)
    }

    /**
     * Solve for optimal KFURL by grid search, minimizing RMSE between
     * Rlsol-predicted load (from pvdks_w) and MAF-derived load (from mshfm_w).
     *
     * **Critical:** KFURL is the slope of the rl(ps) line. At low loads (vacuum),
     * the residual gas offset (KFPRG) dominates and KFURL has minimal influence.
     * A dataset with ~1M idle/cruise samples and ~5K boost samples will cause the
     * solver to minimize RMSE on the low-load majority, producing a KFURL that's
     * too low — even for engines with better-than-stock VE (bigger heads, cams).
     *
     * Verified with real ME7Logger data:
     *   At rpm=3120, rl_w=82.4%, pvdks=1020mbar, pus=833mbar:
     *   - Rlsol(kfurl=0.106) = 76.2% (underpredicts by 6%)
     *   - Rlsol(kfurl=0.115) = 82.6% (matches rl_w)
     *   Without filtering, the solver returns 0.07 (WRONG direction).
     *   With high-load filtering, it correctly returns ~0.115.
     *
     * We filter to samples where pvdks > 0.85 × baro (near-atmospheric or boost),
     * mirroring the Optimizer which only uses WOT entries.
     */
    private fun solveKfurl(
        samples: List<PressureSample>,
        currentKfurl: Double,
        kfprg: Double,
        kfpbrkMap: Map3d?,
        rpmBinWidth: Double
    ): KfurlSuggestion? {
        if (samples.size < 20) return null

        // Filter to WOT samples where pvdks ≈ manifold pressure (ps).
        //
        // CRITICAL: pvdks_w is "Druck Vor DrosselKlappe" — pressure BEFORE the throttle,
        // i.e., compressor outlet pressure. It is ALWAYS >= atmospheric (typically 840–3000 mbar).
        // The Rlsol formula expects MANIFOLD pressure (ps_w, after throttle).
        //
        // At WOT (wdkba >= ~80°), the throttle is fully open, so pvdks ≈ ps and the
        // formula is valid. At part-throttle, pvdks >> ps, and the formula massively
        // over-predicts load, causing the solver to push KFURL artificially DOWN.
        //
        // Solution: Only use WOT samples where pvdks is a valid proxy for ps.
        val wotSamples = samples.filter { it.wdkba >= 80.0 }
        val solveSamples = if (wotSamples.size >= 20) wotSamples else {
            // Fallback: use high-load samples (rl_w > 80%) which are likely WOT even without throttle data
            val highLoadFallback = samples.filter { it.mshfmLoad > 80.0 }
            if (highLoadFallback.size >= 20) highLoadFallback else samples
        }

        println("[AlphaN] KFURL solver: ${samples.size} total → ${wotSamples.size} WOT(>=80°) → using ${solveSamples.size}")

        val searchRange = 0.050..0.200
        val steps = 150

        // Scalar solve on WOT samples
        var bestKfurl = currentKfurl
        var bestRmse = Double.MAX_VALUE

        for (i in 0..steps) {
            val kfurl = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
            val rmse = computeKfurlRmse(solveSamples, kfurl, kfprg, kfpbrkMap)
            if (rmse < bestRmse) {
                bestRmse = rmse
                bestKfurl = kfurl
            }
        }

        val currentRmse = computeKfurlRmse(solveSamples, currentKfurl, kfprg, kfpbrkMap)
        val errorReduction = if (currentRmse > 0) ((currentRmse - bestRmse) / currentRmse) * 100.0 else 0.0

        println("[AlphaN] KFURL result: optimal=${String.format("%.4f", bestKfurl)}, RMSE ${String.format("%.2f", currentRmse)}→${String.format("%.2f", bestRmse)}% (${String.format("%.1f", errorReduction)}% reduction)")

        // Per-RPM solve (also uses WOT-filtered samples)
        val perRpmValues = solveKfurlPerRpm(solveSamples, currentKfurl, kfprg, kfpbrkMap, rpmBinWidth, searchRange, steps)

        return KfurlSuggestion(
            optimalKfurl = bestKfurl,
            currentRmse = currentRmse,
            suggestedRmse = bestRmse,
            errorReductionPercent = errorReduction,
            perRpmValues = perRpmValues
        )
    }

    private fun computeKfurlRmse(
        samples: List<PressureSample>,
        kfurl: Double,
        kfprg: Double,
        kfpbrkMap: Map3d?
    ): Double {
        var totalSqError = 0.0
        for (s in samples) {
            val fpbrkds = kfpbrkMap?.lookup(s.mshfmLoad, s.rpm) ?: DEFAULT_KFPBRK
            val predictedLoad = Rlsol.rlsol(
                pu = s.pus, ps = s.pus, tans = 20.0, tmot = 96.0,
                kfurl = kfurl, plsol = s.pvdks, kfprg = kfprg, fpbrkds = fpbrkds
            )
            val err = predictedLoad - s.mshfmLoad
            totalSqError += err * err
        }
        return sqrt(totalSqError / samples.size)
    }

    private fun solveKfurlPerRpm(
        samples: List<PressureSample>,
        fallbackKfurl: Double,
        kfprg: Double,
        kfpbrkMap: Map3d?,
        rpmBinWidth: Double,
        searchRange: ClosedFloatingPointRange<Double>,
        steps: Int
    ): List<Pair<Double, Double>>? {
        val minRpm = samples.minOf { it.rpm }
        val maxRpm = samples.maxOf { it.rpm }
        if (maxRpm - minRpm < rpmBinWidth) return null

        val perRpmValues = mutableListOf<Pair<Double, Double>>()
        var rc = (minRpm / rpmBinWidth).toInt() * rpmBinWidth + rpmBinWidth / 2
        while (rc <= maxRpm + rpmBinWidth / 2) {
            val binSamples = samples.filter { abs(it.rpm - rc) < rpmBinWidth / 2 }
            if (binSamples.size < 3) {
                perRpmValues.add(rc to fallbackKfurl)
            } else {
                var best = fallbackKfurl
                var bestSq = Double.MAX_VALUE
                for (i in 0..steps) {
                    val kfurl = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
                    var sqErr = 0.0
                    for (s in binSamples) {
                        val fpbrkds = kfpbrkMap?.lookup(s.mshfmLoad, s.rpm) ?: DEFAULT_KFPBRK
                        val pred = Rlsol.rlsol(s.pus, s.pus, 20.0, 96.0, kfurl, s.pvdks, kfprg, fpbrkds)
                        val e = pred - s.mshfmLoad
                        sqErr += e * e
                    }
                    if (sqErr < bestSq) { bestSq = sqErr; best = kfurl }
                }
                perRpmValues.add(rc to best)
            }
            rc += rpmBinWidth
        }
        return if (perRpmValues.size >= 2) perRpmValues else null
    }

    /**
     * Solve for optimal KFPRG by grid search, using the already-solved KFURL.
     */
    private fun solveKfprg(
        samples: List<PressureSample>,
        kfurl: Double,
        currentKfprg: Double,
        kfpbrkMap: Map3d?,
        rpmBinWidth: Double
    ): KfprgSuggestion? {
        if (samples.size < 20) return null

        val searchRange = 20.0..200.0
        val steps = 180

        var bestKfprg = currentKfprg
        var bestRmse = Double.MAX_VALUE

        for (i in 0..steps) {
            val kfprg = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
            val rmse = computeKfprgRmse(samples, kfurl, kfprg, kfpbrkMap)
            if (rmse < bestRmse) {
                bestRmse = rmse
                bestKfprg = kfprg
            }
        }

        val currentRmse = computeKfprgRmse(samples, kfurl, currentKfprg, kfpbrkMap)
        val errorReduction = if (currentRmse > 0) ((currentRmse - bestRmse) / currentRmse) * 100.0 else 0.0

        // Per-RPM solve
        val perRpmValues = solveKfprgPerRpm(samples, kfurl, currentKfprg, kfpbrkMap, rpmBinWidth, searchRange, steps)

        return KfprgSuggestion(
            optimalKfprg = bestKfprg,
            currentRmse = currentRmse,
            suggestedRmse = bestRmse,
            errorReductionPercent = errorReduction,
            perRpmValues = perRpmValues
        )
    }

    private fun computeKfprgRmse(
        samples: List<PressureSample>,
        kfurl: Double,
        kfprg: Double,
        kfpbrkMap: Map3d?
    ): Double {
        var totalSqError = 0.0
        for (s in samples) {
            val fpbrkds = kfpbrkMap?.lookup(s.mshfmLoad, s.rpm) ?: DEFAULT_KFPBRK
            val predictedLoad = Rlsol.rlsol(
                pu = s.pus, ps = s.pus, tans = 20.0, tmot = 96.0,
                kfurl = kfurl, plsol = s.pvdks, kfprg = kfprg, fpbrkds = fpbrkds
            )
            val err = predictedLoad - s.mshfmLoad
            totalSqError += err * err
        }
        return sqrt(totalSqError / samples.size)
    }

    private fun solveKfprgPerRpm(
        samples: List<PressureSample>,
        kfurl: Double,
        fallbackKfprg: Double,
        kfpbrkMap: Map3d?,
        rpmBinWidth: Double,
        searchRange: ClosedFloatingPointRange<Double>,
        steps: Int
    ): List<Pair<Double, Double>>? {
        val minRpm = samples.minOf { it.rpm }
        val maxRpm = samples.maxOf { it.rpm }
        if (maxRpm - minRpm < rpmBinWidth) return null

        val perRpmValues = mutableListOf<Pair<Double, Double>>()
        var rc = (minRpm / rpmBinWidth).toInt() * rpmBinWidth + rpmBinWidth / 2
        while (rc <= maxRpm + rpmBinWidth / 2) {
            val binSamples = samples.filter { abs(it.rpm - rc) < rpmBinWidth / 2 }
            if (binSamples.size < 3) {
                perRpmValues.add(rc to fallbackKfprg)
            } else {
                var best = fallbackKfprg
                var bestSq = Double.MAX_VALUE
                for (i in 0..steps) {
                    val kfprg = searchRange.start + (searchRange.endInclusive - searchRange.start) * i / steps
                    var sqErr = 0.0
                    for (s in binSamples) {
                        val fpbrkds = kfpbrkMap?.lookup(s.mshfmLoad, s.rpm) ?: DEFAULT_KFPBRK
                        val pred = Rlsol.rlsol(s.pus, s.pus, 20.0, 96.0, kfurl, s.pvdks, kfprg, fpbrkds)
                        val e = pred - s.mshfmLoad
                        sqErr += e * e
                    }
                    if (sqErr < bestSq) { bestSq = sqErr; best = kfprg }
                }
                perRpmValues.add(rc to best)
            }
            rc += rpmBinWidth
        }
        return if (perRpmValues.size >= 2) perRpmValues else null
    }

    /**
     * Compute per-RPM KFPBRK correction factors.
     * At each RPM bin: correctionFactor = avg(mshfmLoad / rlsolLoad).
     * Values > 1.0 mean KFPBRK should be increased; < 1.0 means decreased.
     */
    private fun solveKfpbrk(
        samples: List<PressureSample>,
        kfurl: Double,
        kfprg: Double,
        kfpbrkMap: Map3d?,
        rpmBinWidth: Double
    ): List<KfpbrkCorrection>? {
        if (samples.size < 10) return null

        val minRpm = samples.minOf { it.rpm }
        val maxRpm = samples.maxOf { it.rpm }
        val corrections = mutableListOf<KfpbrkCorrection>()

        var rc = (minRpm / rpmBinWidth).toInt() * rpmBinWidth + rpmBinWidth / 2
        while (rc <= maxRpm + rpmBinWidth / 2) {
            val binSamples = samples.filter { abs(it.rpm - rc) < rpmBinWidth / 2 }
            if (binSamples.size >= 3) {
                val ratios = binSamples.mapNotNull { s ->
                    val fpbrkds = kfpbrkMap?.lookup(s.mshfmLoad, s.rpm) ?: DEFAULT_KFPBRK
                    val predictedLoad = Rlsol.rlsol(
                        s.pus, s.pus, 20.0, 96.0, kfurl, s.pvdks, kfprg, fpbrkds
                    )
                    if (predictedLoad > 1.0) s.mshfmLoad / predictedLoad else null
                }
                if (ratios.isNotEmpty()) {
                    val avgRatio = ratios.average()
                    val confidence = when {
                        binSamples.size > 20 -> Confidence.HIGH
                        binSamples.size >= 5 -> Confidence.MEDIUM
                        else -> Confidence.LOW
                    }
                    corrections.add(KfpbrkCorrection(rc, avgRatio, binSamples.size, confidence))
                }
            }
            rc += rpmBinWidth
        }
        return corrections.ifEmpty { null }
    }

    // ── Map3d Output Builders ───────────────────────────────────────

    /**
     * Build a suggested KFURL Map3d from the solver results, preserving the
     * original map's axis structure.
     *
     * KFURL is typically a 1D Kennlinie (RPM → value), but may be stored as:
     * - 1×N (single row, N RPM columns) — RPM on x-axis
     * - N×1 (N RPM rows, single column) — RPM on y-axis
     * - 2D (e.g. cam_angle × RPM) — RPM on x-axis, other parameter on y-axis
     *
     * For each RPM breakpoint on the original axis, we interpolate from the
     * per-RPM solver values. If no per-RPM data, uses the scalar optimal value.
     *
     * **Critical:** Cells that are 0.0 in the original map are preserved as 0.0
     * (these are unused/disabled regions, e.g. cam angle ranges that are not active).
     * Only non-zero cells receive suggested corrections.
     */
    private fun buildSuggestedKfurlMap(
        currentMap: Map3d?,
        suggestion: KfurlSuggestion?
    ): Map3d? {
        if (currentMap == null || suggestion == null) return null
        // Always build the map when we have a valid suggestion.
        // With WOT filtering, even meaningful KFURL corrections may produce
        // small RMSE improvements since WOT data already fits relatively well.
        // Let the UI display results — the user decides whether to apply.

        val z = currentMap.zAxis
        if (z.isEmpty()) return null

        val perRpm = suggestion.perRpmValues
        val scalarValue = suggestion.optimalKfurl

        // Determine orientation: 1×N (row) or N×1 (column) or 2D
        val isRowMap = z.size == 1 && z[0].size > 1
        val isColMap = z.size > 1 && z[0].size == 1

        // Heuristic: determine which axis is RPM.
        // RPM axis values are typically 500–8000. Cam angles / other params are 0–50.
        val xMax = currentMap.xAxis.maxOrNull() ?: 0.0

        val newZ = Array(z.size) { z[it].copyOf() }

        if (isRowMap) {
            val rpmAxis = currentMap.xAxis
            for (col in rpmAxis.indices) {
                if (z[0][col] == 0.0) continue
                newZ[0][col] = interpolateKfurlAtRpm(rpmAxis[col], perRpm, scalarValue)
            }
        } else if (isColMap) {
            val rpmAxis = currentMap.yAxis
            for (row in rpmAxis.indices) {
                if (z[row][0] == 0.0) continue
                newZ[row][0] = interpolateKfurlAtRpm(rpmAxis[row], perRpm, scalarValue)
            }
        } else {
            // 2D map — determine which axis is RPM
            val rpmOnX = xMax > 200  // xAxis has values like 680..6000 (RPM), not 2..43 (cam angle)

            if (rpmOnX) {
                // RPM on x-axis (columns), other parameter (cam angle) on y-axis (rows)
                val rpmAxis = currentMap.xAxis
                for (row in z.indices) {
                    for (col in rpmAxis.indices) {
                        if (z[row][col] == 0.0) continue
                        newZ[row][col] = interpolateKfurlAtRpm(rpmAxis[col], perRpm, scalarValue)
                    }
                }
            } else {
                // RPM on y-axis (rows), other parameter on x-axis (columns)
                val rpmAxis = currentMap.yAxis
                for (row in rpmAxis.indices) {
                    val kfurl = interpolateKfurlAtRpm(rpmAxis[row], perRpm, scalarValue)
                    for (col in newZ[row].indices) {
                        if (z[row][col] == 0.0) continue
                        newZ[row][col] = kfurl
                    }
                }
            }
        }

        return Map3d(currentMap.xAxis, currentMap.yAxis, newZ)
    }

    /**
     * Interpolate a KFURL value at a given RPM from the per-RPM solver results.
     * Uses linear interpolation between the two nearest solver RPM bins.
     * Falls back to the scalar optimal value if no per-RPM data.
     */
    private fun interpolateKfurlAtRpm(
        rpm: Double,
        perRpm: List<Pair<Double, Double>>?,
        scalarFallback: Double
    ): Double {
        if (perRpm == null || perRpm.size < 2) return scalarFallback

        // Clamp to range
        if (rpm <= perRpm.first().first) return perRpm.first().second
        if (rpm >= perRpm.last().first) return perRpm.last().second

        // Find bracketing pair
        for (i in 0 until perRpm.size - 1) {
            val (r0, v0) = perRpm[i]
            val (r1, v1) = perRpm[i + 1]
            if (rpm in r0..r1) {
                val t = if (r1 > r0) (rpm - r0) / (r1 - r0) else 0.0
                return v0 + t * (v1 - v0)
            }
        }
        return scalarFallback
    }

    /**
     * Build a suggested KFPBRK Map3d by applying per-RPM correction factors
     * to the current KFPBRK map.
     *
     * KFPBRK is a 2D map (RPM × load% → correction factor).
     * For each row (RPM breakpoint), we find the nearest solver RPM bin's
     * correction factor and multiply all cells in that row.
     */
    private fun buildSuggestedKfpbrkMap(
        currentMap: Map3d?,
        corrections: List<KfpbrkCorrection>?
    ): Map3d? {
        if (currentMap == null || corrections == null) return null
        if (corrections.isEmpty()) return null
        if (!corrections.any { abs(it.correctionFactor - 1.0) > 0.02 }) return null

        val z = currentMap.zAxis
        if (z.isEmpty()) return null

        val rpmAxis = currentMap.yAxis
        val newZ = Array(z.size) { z[it].copyOf() }

        for (row in rpmAxis.indices) {
            val rpm = rpmAxis[row]
            val factor = interpolateKfpbrkAtRpm(rpm, corrections)
            for (col in newZ[row].indices) {
                if (z[row][col] == 0.0) continue  // preserve zero (unused) cells
                newZ[row][col] = z[row][col] * factor
            }
        }

        return Map3d(currentMap.xAxis, currentMap.yAxis, newZ)
    }

    /**
     * Interpolate a KFPBRK correction factor at a given RPM from the per-RPM solver results.
     * Uses linear interpolation between the two nearest solver RPM bins.
     * Falls back to 1.0 (no correction) if out of range.
     */
    private fun interpolateKfpbrkAtRpm(rpm: Double, corrections: List<KfpbrkCorrection>): Double {
        if (corrections.isEmpty()) return 1.0
        if (corrections.size == 1) return corrections[0].correctionFactor

        val sorted = corrections.sortedBy { it.rpmCenter }

        if (rpm <= sorted.first().rpmCenter) return sorted.first().correctionFactor
        if (rpm >= sorted.last().rpmCenter) return sorted.last().correctionFactor

        for (i in 0 until sorted.size - 1) {
            val c0 = sorted[i]
            val c1 = sorted[i + 1]
            if (rpm in c0.rpmCenter..c1.rpmCenter) {
                val t = if (c1.rpmCenter > c0.rpmCenter) (rpm - c0.rpmCenter) / (c1.rpmCenter - c0.rpmCenter) else 0.0
                return c0.correctionFactor + t * (c1.correctionFactor - c0.correctionFactor)
            }
        }
        return 1.0
    }

    // ── Error Classification ──────────────────────────────────────────

    /**
     * Classify the dominant error type between mshfm_w and msdk_w.
     */
    private fun classifyErrorType(
        @Suppress("UNUSED_PARAMETER") samples: List<Sample>,
        rpmBins: List<RpmBinResult>
    ): ErrorType {
        if (rpmBins.size < 2) return ErrorType.INSUFFICIENT_DATA

        val binErrors = rpmBins.filter { it.sampleCount >= 3 }.map { it.avgErrorPercent }
        if (binErrors.size < 2) return ErrorType.INSUFFICIENT_DATA

        val errorRange = (binErrors.maxOrNull() ?: 0.0) - (binErrors.minOrNull() ?: 0.0)
        val errorMean = abs(binErrors.average())

        val absErrors = rpmBins.filter { it.sampleCount >= 3 }.map { it.avgErrorAbsolute }
        val absRange = (absErrors.maxOrNull() ?: 0.0) - (absErrors.minOrNull() ?: 0.0)

        val absVariationCoeff = if (abs(absErrors.average()) > 0.1)
            absRange / abs(absErrors.average()) else 100.0
        val pctVariationCoeff = if (errorMean > 0.1)
            errorRange / errorMean else 100.0

        return when {
            errorRange > 10.0 -> ErrorType.RPM_DEPENDENT
            absVariationCoeff < pctVariationCoeff * 0.5 -> ErrorType.ADDITIVE
            pctVariationCoeff < 0.5 -> ErrorType.MULTIPLICATIVE
            else -> ErrorType.MIXED
        }
    }

    // ── Recommendations Builder ───────────────────────────────────────

    private fun buildRecommendations(
        severity: Severity,
        errorType: ErrorType,
        avgError: Double,
        rpmBins: List<RpmBinResult>,
        multiplicativeCorrection: Double?,
        additiveCorrection: Double?,
        kfurlSuggestion: KfurlSuggestion?,
        kfprgSuggestion: KfprgSuggestion?,
        kfpbrkCorrections: List<KfpbrkCorrection>?,
        hasPressureData: Boolean
    ): List<String> {
        val recs = mutableListOf<String>()

        when (severity) {
            Severity.GOOD -> {
                recs.add("✅ Alpha-n accuracy is good (avg ${String.format("%.1f", abs(avgError))}% error). " +
                    "The car should run smoothly if the MAF is unplugged.")
                recs.add("Ensure adaptations (msndko_w, fkmsdk_w) are fully learned before unplugging MAF.")
            }
            Severity.WARNING -> {
                recs.add("⚠️ Alpha-n has moderate deviation (avg ${String.format("%.1f", abs(avgError))}% error). " +
                    "The car will run but may be rough at some operating points.")
            }
            Severity.CRITICAL -> {
                recs.add("🔴 Alpha-n has significant deviation (avg ${String.format("%.1f", abs(avgError))}% error). " +
                    "The car will likely run poorly with MAF unplugged.")
            }
        }

        when (errorType) {
            ErrorType.ADDITIVE -> {
                recs.add("Error type: ADDITIVE — constant offset regardless of airflow.")
                recs.add("Root cause: msndko_w (learned leak air) or MSLG (static leak air constant) is incorrect.")
                if (additiveCorrection != null) {
                    recs.add("Estimated correction: ${String.format("%.1f", additiveCorrection)} kg/h additive offset.")
                }
                recs.add("Action: Reset ECU adaptations and let msndko_w re-learn with MAF connected.")
                recs.add("If error persists after re-learning, check for vacuum leaks or incorrect MSLG.")
            }
            ErrorType.MULTIPLICATIVE -> {
                recs.add("Error type: MULTIPLICATIVE — error scales with airflow.")
                recs.add("Root cause: fkmsdk_w (learned multiplier) or KFMSNWDK (throttle body flow map) is incorrect.")
                if (multiplicativeCorrection != null) {
                    recs.add("Estimated correction factor: ${String.format("%.3f", multiplicativeCorrection)} " +
                        "(${String.format("%.1f", (multiplicativeCorrection - 1.0) * 100)}% adjustment needed).")
                }
                recs.add("Action: Reset ECU adaptations and let fkmsdk_w re-learn with MAF connected.")
                recs.add("If fkmsdk_w hits its limit (check FKMSDKMX/FKMSDKMN), KFMSNWDK needs re-calibration.")
            }
            ErrorType.RPM_DEPENDENT -> {
                recs.add("Error type: RPM-DEPENDENT — varies across RPM range.")
                recs.add("Root cause: KFURL (VE slope) or KFPBRK (VE correction) needs RPM-specific adjustment.")

                // Find the worst RPM bins
                val worstBins = rpmBins.filter { it.sampleCount >= 3 }
                    .sortedByDescending { abs(it.avgErrorPercent) }
                    .take(3)
                if (worstBins.isNotEmpty()) {
                    recs.add("Worst RPM ranges:")
                    for (bin in worstBins) {
                        recs.add("  ${bin.rpmCenter.toInt()} RPM: ${String.format("%.1f", bin.avgErrorPercent)}% " +
                            "error (${bin.sampleCount} samples)")
                    }
                }
                recs.add("Action: Use Optimizer to analyze KFURL per-RPM and KFPBRK corrections.")
                recs.add("Note: This error type suggests cams, head, or intake changes that altered VE at specific RPMs.")
            }
            ErrorType.MIXED -> {
                recs.add("Error type: MIXED — combination of additive and multiplicative errors.")
                recs.add("Action: Reset ECU adaptations first. If error persists, review KFMSNWDK and BGSRM VE maps.")
                if (multiplicativeCorrection != null) {
                    recs.add("Estimated multiplicative correction: ${String.format("%.3f", multiplicativeCorrection)}")
                }
                if (additiveCorrection != null) {
                    recs.add("Estimated additive correction: ${String.format("%.1f", additiveCorrection)} kg/h")
                }
            }
            ErrorType.INSUFFICIENT_DATA -> {
                recs.add("Insufficient data for classification. Log more operating points with both mshfm_w and msdk_w.")
            }
        }

        // ── VE Model Suggestions ──────────────────────────────────────
        if (kfurlSuggestion != null && kfurlSuggestion.errorReductionPercent > 2.0) {
            recs.add("")
            recs.add("── VE Model Suggestions (from pressure data) ──")
            recs.add("KFURL: current ~${String.format("%.4f", DEFAULT_KFURL)}, " +
                "suggested ${String.format("%.4f", kfurlSuggestion.optimalKfurl)} " +
                "(${String.format("%.1f", kfurlSuggestion.errorReductionPercent)}% VE error reduction)")
            val perRpm = kfurlSuggestion.perRpmValues
            if (perRpm != null && perRpm.size >= 2) {
                val range = perRpm.maxOf { it.second } - perRpm.minOf { it.second }
                if (range > 0.005) {
                    recs.add("KFURL varies with RPM (range ${String.format("%.4f", range)}):")
                    for ((rpm, kfurl) in perRpm) {
                        recs.add("  ${rpm.toInt()} RPM → ${String.format("%.4f", kfurl)}")
                    }
                }
            }
        }

        if (kfprgSuggestion != null && kfprgSuggestion.errorReductionPercent > 2.0) {
            recs.add("KFPRG: current ~${String.format("%.0f", DEFAULT_KFPRG)} hPa, " +
                "suggested ${String.format("%.0f", kfprgSuggestion.optimalKfprg)} hPa " +
                "(${String.format("%.1f", kfprgSuggestion.errorReductionPercent)}% VE error reduction)")
            recs.add("ℹ️ Alpha-n data includes part-throttle points — KFPRG suggestion may be more " +
                "accurate than WOT-only Optimizer results (residual gas is dominant at low load).")
            val perRpm = kfprgSuggestion.perRpmValues
            if (perRpm != null && perRpm.size >= 2) {
                val range = perRpm.maxOf { it.second } - perRpm.minOf { it.second }
                if (range > 5.0) {
                    recs.add("KFPRG varies with RPM (range ${String.format("%.0f", range)} hPa):")
                    for ((rpm, kfprg) in perRpm) {
                        recs.add("  ${rpm.toInt()} RPM → ${String.format("%.0f", kfprg)} hPa")
                    }
                }
            }
        }

        if (kfpbrkCorrections != null && kfpbrkCorrections.any { abs(it.correctionFactor - 1.0) > 0.02 }) {
            recs.add("KFPBRK per-RPM correction factors:")
            for (c in kfpbrkCorrections) {
                val confLabel = when (c.confidence) {
                    Confidence.HIGH -> "HIGH"
                    Confidence.MEDIUM -> "MEDIUM"
                    Confidence.LOW -> "LOW"
                }
                recs.add("  ${c.rpmCenter.toInt()} RPM → ×${String.format("%.3f", c.correctionFactor)} " +
                    "(${c.sampleCount} samples, $confLabel confidence)")
            }
            recs.add("⚠️ KFPBRK suggestions are preliminary — verify with Optimizer WOT logs for higher confidence.")
        }

        if (!hasPressureData && severity != Severity.GOOD) {
            recs.add("")
            recs.add("ℹ️ Log pvdks_w and pus_w to enable VE model suggestions (KFURL, KFPRG, KFPBRK corrections).")
        }

        // Always add the WDKUGDN clarification
        if (severity != Severity.GOOD) {
            recs.add("")
            recs.add("⚠️ Note: Do NOT adjust WDKUGDN to fix alpha-n accuracy. WDKUGDN defines the throttle " +
                "body choke point — only change it if you've changed the physical throttle body diameter. " +
                "See documentation/me7-alpha-n-calibration.md for details.")
        }

        return recs
    }
}

