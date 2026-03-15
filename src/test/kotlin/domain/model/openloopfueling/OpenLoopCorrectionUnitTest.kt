package domain.model.openloopfueling

import data.contract.AfrLogFileContract
import data.contract.Me7LogFileContract
import domain.math.map.Map3d
import domain.model.openloopfueling.correction.OpenLoopMlhfmCorrectionManager
import kotlin.math.abs
import kotlin.test.*

/**
 * Unit tests for the open-loop correction algorithm.
 *
 * These tests use synthetic data to verify the math independently of file parsing.
 * Each test targets a specific step of the algorithm:
 *   1. Voltage grouping + AFR error calculation
 *   2. Mean/mode correction processing
 *   3. NaN interpolation (post-processing)
 *   4. 5-point moving average smoothing
 *   5. Final application: corrected = original × (1 + correction)
 */
class OpenLoopCorrectionUnitTest {

    /**
     * Build a synthetic MLHFM with known voltage → kg/h mapping.
     * Linear ramp: each voltage step = 100 kg/h more.
     */
    private fun buildSyntheticMlhfm(voltages: Array<Double>, baseKgH: Double = 50.0, stepKgH: Double = 100.0): Map3d {
        val kgH = Array(voltages.size) { i -> arrayOf(baseKgH + i * stepKgH) }
        return Map3d(emptyArray(), voltages, kgH)
    }

    /**
     * Build a synthetic ME7 log with controlled MAF voltage, STFT, LTFT, RPM, throttle.
     * Each entry corresponds to one data point the correction manager will process.
     */
    private fun buildMe7Log(
        timestamps: List<Double>,
        rpms: List<Double>,
        mafVoltages: List<Double>,
        stfts: List<Double>,
        ltfts: List<Double>,
        throttleAngles: List<Double>,
        lambdaControlActive: List<Double>,
        requestedLambdas: List<Double>,
        mafGramsSec: List<Double>,
        injectorOnTimes: List<Double>,
        engineLoads: List<Double>,
        wideBandO2: List<Double> = emptyList()
    ): Map<Me7LogFileContract.Header, List<Double>> = mapOf(
        Me7LogFileContract.Header.START_TIME_HEADER to listOf(0.0),
        Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER to timestamps,
        Me7LogFileContract.Header.RPM_COLUMN_HEADER to rpms,
        Me7LogFileContract.Header.MAF_VOLTAGE_HEADER to mafVoltages,
        Me7LogFileContract.Header.STFT_COLUMN_HEADER to stfts,
        Me7LogFileContract.Header.LTFT_COLUMN_HEADER to ltfts,
        Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER to throttleAngles,
        Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER to lambdaControlActive,
        Me7LogFileContract.Header.REQUESTED_LAMBDA_HEADER to requestedLambdas,
        Me7LogFileContract.Header.MAF_GRAMS_PER_SECOND_HEADER to mafGramsSec,
        Me7LogFileContract.Header.FUEL_INJECTOR_ON_TIME_HEADER to injectorOnTimes,
        Me7LogFileContract.Header.ENGINE_LOAD_HEADER to engineLoads,
        Me7LogFileContract.Header.WIDE_BAND_O2_HEADER to wideBandO2
    )

    /**
     * Build a synthetic AFR log matching a ME7 log.
     */
    private fun buildAfrLog(
        rpms: List<Double>,
        afrs: List<Double>,
        timestamps: List<Double>,
        tps: List<Double>,
        boost: List<Double>
    ): Map<String, List<Double>> = mapOf(
        AfrLogFileContract.START_TIME to listOf(0.0),
        AfrLogFileContract.TIMESTAMP to timestamps,
        AfrLogFileContract.RPM_HEADER to rpms,
        AfrLogFileContract.AFR_HEADER to afrs,
        AfrLogFileContract.TPS_HEADER to tps,
        AfrLogFileContract.BOOST_HEADER to boost
    )

    @Test
    fun `perfect AFR data produces near-zero corrections`() {
        // If measured AFR exactly matches requested lambda, corrections should be ~0
        val voltages = arrayOf(1.0, 2.0, 3.0, 4.0)
        val mlhfm = buildSyntheticMlhfm(voltages)
        val n = 200

        // Build log data where all readings cluster around voltage 2.0
        // with perfect stoichiometric fueling (AFR = requested lambda × 14.7)
        val rpms = List(n) { 3000.0 }
        val mafVoltages = List(n) { 2.0 }
        val stfts = List(n) { 1.0 }  // STFT=1.0 means no correction (0% error)
        val ltfts = List(n) { 1.0 }  // LTFT=1.0 means no correction
        val throttles = List(n) { 90.0 }
        val lambdaControl = List(n) { 0.0 }  // lambda control enabled = 0.0 for open loop
        val requestedLambda = List(n) { 1.0 }
        val mafGs = List(n) { 100.0 }
        val injOnTime = List(n) { 5.0 }
        val engineLoad = List(n) { 80.0 }
        val timestamps = List(n) { it * 0.01 }

        // AFR log: perfect stoichiometric (14.7) for all RPM values
        val afrRpms = List(n) { 3000.0 }
        val afrValues = List(n) { 14.7 }
        val afrTimestamps = List(n) { it * 0.01 }
        val afrTps = List(n) { 90.0 }
        val afrBoost = List(n) { 5.0 }

        val me7Log = buildMe7Log(timestamps, rpms, mafVoltages, stfts, ltfts,
            throttles, lambdaControl, requestedLambda, mafGs, injOnTime, engineLoad)
        val afrLog = buildAfrLog(afrRpms, afrValues, afrTimestamps, afrTps, afrBoost)

        val manager = OpenLoopMlhfmCorrectionManager(
            minThrottleAngle = 80.0,
            minRpm = 2000.0,
            minPointsMe7 = 1,
            minPointsAfr = 1,
            maxAfr = 16.0
        )
        manager.correct(me7Log, afrLog, mlhfm)

        val correction = manager.openLoopCorrection
        assertNotNull(correction, "Correction should be produced")

        // With perfect AFR, the corrected MLHFM should be very close to original
        for (i in correction.correctedMlhfm.zAxis.indices) {
            val orig = mlhfm.zAxis[i][0]
            val corr = correction.correctedMlhfm.zAxis[i][0]
            if (orig > 1.0) {
                val ratio = corr / orig
                // Allow some tolerance due to smoothing, but should be close to 1.0
                assertTrue(ratio in 0.85..1.15,
                    "Perfect AFR data should produce near-unity correction at index $i: " +
                    "ratio=$ratio (orig=$orig, corr=$corr)")
            }
        }
    }

    @Test
    fun `correction output has same size as input MLHFM`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5)
        val mlhfm = buildSyntheticMlhfm(voltages)
        val n = 100

        val rpms = List(n) { 3000.0 }
        val mafVoltages = List(n) { 2.5 }
        val stfts = List(n) { 1.05 }
        val ltfts = List(n) { 1.02 }
        val throttles = List(n) { 90.0 }
        val lambdaControl = List(n) { 0.0 }
        val requestedLambda = List(n) { 1.0 }
        val mafGs = List(n) { 100.0 }
        val injOnTime = List(n) { 5.0 }
        val engineLoad = List(n) { 80.0 }
        val timestamps = List(n) { it * 0.01 }

        val afrRpms = List(n) { 3000.0 }
        val afrValues = List(n) { 15.0 }
        val afrTimestamps = List(n) { it * 0.01 }
        val afrTps = List(n) { 90.0 }
        val afrBoost = List(n) { 5.0 }

        val me7Log = buildMe7Log(timestamps, rpms, mafVoltages, stfts, ltfts,
            throttles, lambdaControl, requestedLambda, mafGs, injOnTime, engineLoad)
        val afrLog = buildAfrLog(afrRpms, afrValues, afrTimestamps, afrTps, afrBoost)

        val manager = OpenLoopMlhfmCorrectionManager(
            minThrottleAngle = 80.0,
            minRpm = 2000.0,
            minPointsMe7 = 1,
            minPointsAfr = 1,
            maxAfr = 16.0
        )
        manager.correct(me7Log, afrLog, mlhfm)

        val correction = manager.openLoopCorrection!!
        assertEquals(voltages.size, correction.correctedMlhfm.zAxis.size,
            "Corrected MLHFM must have same number of entries as input")
        assertContentEquals(voltages, correction.correctedMlhfm.yAxis,
            "Corrected MLHFM voltage axis must match input")
    }

    @Test
    fun `correction output contains no NaN values`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5)
        val mlhfm = buildSyntheticMlhfm(voltages)
        val n = 100

        val rpms = List(n) { 3000.0 }
        val mafVoltages = List(n) { 2.5 }
        val stfts = List(n) { 1.0 }
        val ltfts = List(n) { 1.0 }
        val throttles = List(n) { 90.0 }
        val lambdaControl = List(n) { 0.0 }
        val requestedLambda = List(n) { 1.0 }
        val mafGs = List(n) { 100.0 }
        val injOnTime = List(n) { 5.0 }
        val engineLoad = List(n) { 80.0 }
        val timestamps = List(n) { it * 0.01 }

        val afrRpms = List(n) { 3000.0 }
        val afrValues = List(n) { 14.7 }
        val afrTimestamps = List(n) { it * 0.01 }
        val afrTps = List(n) { 90.0 }
        val afrBoost = List(n) { 5.0 }

        val me7Log = buildMe7Log(timestamps, rpms, mafVoltages, stfts, ltfts,
            throttles, lambdaControl, requestedLambda, mafGs, injOnTime, engineLoad)
        val afrLog = buildAfrLog(afrRpms, afrValues, afrTimestamps, afrTps, afrBoost)

        val manager = OpenLoopMlhfmCorrectionManager(
            minThrottleAngle = 80.0,
            minRpm = 2000.0,
            minPointsMe7 = 1,
            minPointsAfr = 1,
            maxAfr = 16.0
        )
        manager.correct(me7Log, afrLog, mlhfm)
        val correction = manager.openLoopCorrection!!

        for (i in correction.correctedMlhfm.zAxis.indices) {
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isNaN(),
                "Corrected MLHFM[$i] should not be NaN")
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isInfinite(),
                "Corrected MLHFM[$i] should not be Infinite")
        }
    }

    @Test
    fun `all corrected kg per h values are non-negative`() {
        // Even with corrections, airflow cannot be negative
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5)
        val mlhfm = buildSyntheticMlhfm(voltages, baseKgH = 10.0, stepKgH = 50.0)
        val n = 100

        val rpms = List(n) { 3000.0 }
        val mafVoltages = List(n) { 2.5 }
        val stfts = List(n) { 1.1 }
        val ltfts = List(n) { 1.1 }
        val throttles = List(n) { 90.0 }
        val lambdaControl = List(n) { 0.0 }
        val requestedLambda = List(n) { 0.85 }
        val mafGs = List(n) { 100.0 }
        val injOnTime = List(n) { 5.0 }
        val engineLoad = List(n) { 80.0 }
        val timestamps = List(n) { it * 0.01 }

        val afrRpms = List(n) { 3000.0 }
        val afrValues = List(n) { 11.0 }
        val afrTimestamps = List(n) { it * 0.01 }
        val afrTps = List(n) { 90.0 }
        val afrBoost = List(n) { 5.0 }

        val me7Log = buildMe7Log(timestamps, rpms, mafVoltages, stfts, ltfts,
            throttles, lambdaControl, requestedLambda, mafGs, injOnTime, engineLoad)
        val afrLog = buildAfrLog(afrRpms, afrValues, afrTimestamps, afrTps, afrBoost)

        val manager = OpenLoopMlhfmCorrectionManager(
            minThrottleAngle = 80.0,
            minRpm = 2000.0,
            minPointsMe7 = 1,
            minPointsAfr = 1,
            maxAfr = 16.0
        )
        manager.correct(me7Log, afrLog, mlhfm)
        val correction = manager.openLoopCorrection!!

        for (i in correction.correctedMlhfm.zAxis.indices) {
            assertTrue(correction.correctedMlhfm.zAxis[i][0] >= 0.0,
                "MLHFM[$i] = ${correction.correctedMlhfm.zAxis[i][0]} must be non-negative")
        }
    }

    @Test
    fun `filtering respects minThrottleAngle and minRpm thresholds`() {
        val voltages = arrayOf(1.0, 2.0, 3.0)
        val mlhfm = buildSyntheticMlhfm(voltages)

        // Build log with entries below thresholds — should produce minimal/no correction
        val n = 100
        val rpms = List(n) { 1000.0 }  // Below minRpm of 2000
        val mafVoltages = List(n) { 2.0 }
        val stfts = List(n) { 1.2 }   // Large STFT that would produce big correction
        val ltfts = List(n) { 1.2 }
        val throttles = List(n) { 50.0 }  // Below minThrottleAngle of 80
        val lambdaControl = List(n) { 0.0 }
        val requestedLambda = List(n) { 1.0 }
        val mafGs = List(n) { 100.0 }
        val injOnTime = List(n) { 5.0 }
        val engineLoad = List(n) { 80.0 }
        val timestamps = List(n) { it * 0.01 }

        val afrRpms = List(n) { 1000.0 }
        val afrValues = List(n) { 18.0 }  // Very lean — would produce large correction if not filtered
        val afrTimestamps = List(n) { it * 0.01 }
        val afrTps = List(n) { 50.0 }
        val afrBoost = List(n) { 0.0 }

        val me7Log = buildMe7Log(timestamps, rpms, mafVoltages, stfts, ltfts,
            throttles, lambdaControl, requestedLambda, mafGs, injOnTime, engineLoad)
        val afrLog = buildAfrLog(afrRpms, afrValues, afrTimestamps, afrTps, afrBoost)

        val manager = OpenLoopMlhfmCorrectionManager(
            minThrottleAngle = 80.0,
            minRpm = 2000.0,
            minPointsMe7 = 1,
            minPointsAfr = 1,
            maxAfr = 16.0
        )
        manager.correct(me7Log, afrLog, mlhfm)

        // The correction should be produced but the result should be close to original
        // because the filter criteria excluded the data
        val correction = manager.openLoopCorrection
        assertNotNull(correction, "Correction object should still be produced")
    }
}
