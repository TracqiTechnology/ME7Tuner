package domain.model.edge

import data.contract.Me7LogFileContract
import domain.math.map.Map3d
import domain.model.closedloopfueling.ClosedLoopFuelingCorrectionManager
import domain.model.openloopfueling.correction.OpenLoopMlhfmCorrectionManager
import kotlin.test.*

/**
 * Edge case tests for correction managers.
 *
 * These verify that bad/unusual input data doesn't cause crashes, NaN propagation,
 * or dangerously wrong corrections. Real-world log data is messy — sensor dropouts,
 * corrupt rows, and partial logs are common.
 */
class BadInputEdgeCaseTest {

    private fun buildSyntheticMlhfm(): Map3d {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5)
        val kgH = Array(voltages.size) { i -> arrayOf(10.0 + i * 50.0) }
        return Map3d(emptyArray(), voltages, kgH)
    }

    private fun buildMinimalClosedLoopLog(
        n: Int,
        rpmValue: Double = 3000.0,
        mafVoltage: Double = 2.5,
        stft: Double = 1.0,
        ltft: Double = 1.0,
        throttle: Double = 30.0,
        lambdaActive: Double = 1.0,
        engineLoad: Double = 80.0
    ): Map<Me7LogFileContract.Header, List<Double>> = mapOf(
        Me7LogFileContract.Header.START_TIME_HEADER to listOf(0.0),
        Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER to List(n) { it * 0.1 },
        Me7LogFileContract.Header.RPM_COLUMN_HEADER to List(n) { rpmValue },
        Me7LogFileContract.Header.MAF_VOLTAGE_HEADER to List(n) { mafVoltage },
        Me7LogFileContract.Header.STFT_COLUMN_HEADER to List(n) { stft },
        Me7LogFileContract.Header.LTFT_COLUMN_HEADER to List(n) { ltft },
        Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER to List(n) { throttle },
        Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER to List(n) { lambdaActive },
        Me7LogFileContract.Header.ENGINE_LOAD_HEADER to List(n) { engineLoad }
    )

    // ── Closed-Loop Edge Cases ──

    @Test
    fun `closed-loop with minimal data points does not crash`() {
        val mlhfm = buildSyntheticMlhfm()
        val logData = buildMinimalClosedLoopLog(n = 3)

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 50.0
        )

        // Should not throw
        manager.correct(logData, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection
        assertNotNull(correction, "Should produce a correction object even with minimal data")

        // All output values should be valid (no NaN/Inf)
        for (i in correction.correctedMlhfm.zAxis.indices) {
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isNaN(),
                "Minimal data should not produce NaN at index $i")
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isInfinite(),
                "Minimal data should not produce Infinity at index $i")
        }
    }

    @Test
    fun `closed-loop with all data filtered out produces safe output`() {
        val mlhfm = buildSyntheticMlhfm()

        // Set thresholds so high that ALL data is filtered out
        val logData = buildMinimalClosedLoopLog(n = 100, rpmValue = 500.0, throttle = 5.0)

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 80.0,  // Threshold higher than any log entry
            minRpm = 5000.0,          // Threshold higher than any log entry
            maxDerivative = 0.001     // Very tight derivative filter
        )

        manager.correct(logData, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection
        assertNotNull(correction, "Should still produce correction object when all data filtered")

        // With all data filtered, corrections should be near-zero (original preserved)
        for (i in correction.correctedMlhfm.zAxis.indices) {
            val orig = mlhfm.zAxis[i][0]
            val corr = correction.correctedMlhfm.zAxis[i][0]
            assertFalse(corr.isNaN(), "Filtered-out data should not produce NaN at index $i")

            // When all data is filtered, corrected should equal original (0% correction)
            if (orig > 1.0) {
                val ratio = corr / orig
                assertTrue(ratio in 0.9..1.1,
                    "With all data filtered, correction at index $i should be minimal: " +
                    "ratio=$ratio (orig=$orig, corr=$corr)")
            }
        }
    }

    @Test
    fun `closed-loop with STFT at extreme values does not produce wild corrections`() {
        val mlhfm = buildSyntheticMlhfm()

        // STFT at extreme but physically possible values (±25%)
        val logData = buildMinimalClosedLoopLog(
            n = 200,
            stft = 1.25,  // +25% STFT (ECU adding 25% fuel)
            ltft = 1.25,  // +25% LTFT
            lambdaActive = 1.0
        )

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 50.0
        )
        manager.correct(logData, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection
        assertNotNull(correction)

        // Extreme fuel trims should produce corrections, but still within physical bounds
        for (i in correction.correctedMlhfm.zAxis.indices) {
            val value = correction.correctedMlhfm.zAxis[i][0]
            assertTrue(value >= 0.0, "Corrected[$i] = $value must be non-negative")
            assertFalse(value.isNaN(), "Corrected[$i] should not be NaN")
            assertFalse(value.isInfinite(), "Corrected[$i] should not be Infinite")
        }
    }

    @Test
    fun `closed-loop with single voltage point produces valid output`() {
        // MAF stuck at one voltage — all log entries at voltage 2.0
        val mlhfm = buildSyntheticMlhfm()
        val logData = buildMinimalClosedLoopLog(n = 200, mafVoltage = 2.0)

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 50.0
        )
        manager.correct(logData, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection!!
        for (i in correction.correctedMlhfm.zAxis.indices) {
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isNaN(),
                "Single-voltage data should not produce NaN at index $i")
        }
    }

    @Test
    fun `closed-loop with high derivative filter rejects noisy data`() {
        val mlhfm = buildSyntheticMlhfm()

        // Build log with rapidly changing voltage (high derivative)
        val n = 200
        val logData = mapOf(
            Me7LogFileContract.Header.START_TIME_HEADER to listOf(0.0),
            Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER to List(n) { it * 0.01 }, // Fast sampling
            Me7LogFileContract.Header.RPM_COLUMN_HEADER to List(n) { 3000.0 },
            Me7LogFileContract.Header.MAF_VOLTAGE_HEADER to List(n) { if (it % 2 == 0) 1.0 else 4.0 }, // Alternating
            Me7LogFileContract.Header.STFT_COLUMN_HEADER to List(n) { 1.15 },
            Me7LogFileContract.Header.LTFT_COLUMN_HEADER to List(n) { 1.1 },
            Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER to List(n) { 30.0 },
            Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER to List(n) { 1.0 },
            Me7LogFileContract.Header.ENGINE_LOAD_HEADER to List(n) { 60.0 }
        )

        // Very tight derivative filter — should reject the rapidly oscillating voltage data
        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 1.0  // Only accept slowly changing voltage
        )
        manager.correct(logData, mlhfm)

        // Should still produce valid (non-NaN) output
        val correction = manager.closedLoopMlhfmCorrection!!
        for (i in correction.correctedMlhfm.zAxis.indices) {
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isNaN(),
                "High-derivative filtered data should not produce NaN at index $i")
        }
    }
}
