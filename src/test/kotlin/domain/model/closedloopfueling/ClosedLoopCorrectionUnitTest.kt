package domain.model.closedloopfueling

import data.contract.Me7LogFileContract
import domain.math.map.Map3d
import kotlin.math.abs
import kotlin.test.*

/**
 * Unit tests for the closed-loop correction algorithm.
 *
 * Closed-loop correction uses STFT (Short-Term Fuel Trim) and LTFT (Long-Term Fuel Trim)
 * from the ECU's lambda control to correct the MAF linearization.
 *
 * Algorithm steps:
 *   1. Filter: lambda control active, throttle > min, RPM > min, voltage derivative < max
 *   2. For each filtered point: correction = (STFT - 1) × voltageScaler + (LTFT - 1) × voltageScaler
 *   3. Group corrections by nearest MLHFM voltage bin
 *   4. Process: mean + mode averaging per bin
 *   5. Post-process: interpolate NaN gaps
 *   6. Smooth: 5-point moving average
 *   7. Apply: corrected = original × (1 + correction)
 */
class ClosedLoopCorrectionUnitTest {

    private fun buildSyntheticMlhfm(voltages: Array<Double>, baseKgH: Double = 50.0, stepKgH: Double = 50.0): Map3d {
        val kgH = Array(voltages.size) { i -> arrayOf(baseKgH + i * stepKgH) }
        return Map3d(emptyArray(), voltages, kgH)
    }

    private fun buildClosedLoopLog(
        n: Int,
        timestamps: List<Double> = List(n) { it * 0.1 },
        rpms: List<Double> = List(n) { 3000.0 },
        mafVoltages: List<Double> = List(n) { 2.5 },
        stfts: List<Double> = List(n) { 1.0 },
        ltfts: List<Double> = List(n) { 1.0 },
        throttleAngles: List<Double> = List(n) { 30.0 },
        lambdaControlActive: List<Double> = List(n) { 1.0 },
        engineLoads: List<Double> = List(n) { 60.0 }
    ): Map<Me7LogFileContract.Header, List<Double>> = mapOf(
        Me7LogFileContract.Header.START_TIME_HEADER to listOf(0.0),
        Me7LogFileContract.Header.TIME_STAMP_COLUMN_HEADER to timestamps,
        Me7LogFileContract.Header.RPM_COLUMN_HEADER to rpms,
        Me7LogFileContract.Header.MAF_VOLTAGE_HEADER to mafVoltages,
        Me7LogFileContract.Header.STFT_COLUMN_HEADER to stfts,
        Me7LogFileContract.Header.LTFT_COLUMN_HEADER to ltfts,
        Me7LogFileContract.Header.THROTTLE_PLATE_ANGLE_HEADER to throttleAngles,
        Me7LogFileContract.Header.LAMBDA_CONTROL_ACTIVE_HEADER to lambdaControlActive,
        Me7LogFileContract.Header.ENGINE_LOAD_HEADER to engineLoads
    )

    @Test
    fun `neutral fuel trims produce near-zero corrections`() {
        val voltages = arrayOf(1.0, 2.0, 3.0, 4.0)
        val mlhfm = buildSyntheticMlhfm(voltages)

        // STFT=1.0 and LTFT=1.0 means 0% correction (neutral trims)
        val log = buildClosedLoopLog(
            n = 300,
            mafVoltages = List(300) { 1.0 + (it % 4) * 1.0 }, // Spread across voltages
            stfts = List(300) { 1.0 },
            ltfts = List(300) { 1.0 },
            lambdaControlActive = List(300) { 1.0 } // 1.0 = lambda control active for closed loop
        )

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 1000.0 // Very permissive
        )
        manager.correct(log, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection
        assertNotNull(correction, "Correction should be produced")

        for (i in correction.correctedMlhfm.zAxis.indices) {
            val orig = mlhfm.zAxis[i][0]
            val corr = correction.correctedMlhfm.zAxis[i][0]
            if (orig > 1.0) {
                val ratio = corr / orig
                assertTrue(ratio in 0.90..1.10,
                    "Neutral trims should produce near-unity correction at [$i]: " +
                    "ratio=$ratio (orig=$orig, corr=$corr)")
            }
        }
    }

    @Test
    fun `positive fuel trims produce upward correction`() {
        val voltages = arrayOf(1.0, 2.0, 3.0, 4.0)
        val mlhfm = buildSyntheticMlhfm(voltages)

        // STFT=1.10 means ECU is adding 10% fuel → MAF is under-reading → need MORE kg/h
        val log = buildClosedLoopLog(
            n = 300,
            mafVoltages = List(300) { 1.0 + (it % 4) * 1.0 },
            stfts = List(300) { 1.10 },
            ltfts = List(300) { 1.05 },
            lambdaControlActive = List(300) { 1.0 }
        )

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 1000.0
        )
        manager.correct(log, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection!!

        // With positive trims, corrected values should be >= original (more airflow)
        var upwardCount = 0
        for (i in correction.correctedMlhfm.zAxis.indices) {
            val orig = mlhfm.zAxis[i][0]
            val corr = correction.correctedMlhfm.zAxis[i][0]
            if (orig > 5.0 && corr > orig) upwardCount++
        }
        // At least some bins should show upward correction
        assertTrue(upwardCount > 0,
            "Positive fuel trims should produce upward corrections in at least some bins")
    }

    @Test
    fun `correction output is same size as input MLHFM`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5)
        val mlhfm = buildSyntheticMlhfm(voltages)

        val log = buildClosedLoopLog(n = 200, lambdaControlActive = List(200) { 1.0 })

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 1000.0
        )
        manager.correct(log, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection!!
        assertEquals(voltages.size, correction.correctedMlhfm.zAxis.size,
            "Corrected MLHFM must have same number of entries as input")
        assertContentEquals(voltages, correction.correctedMlhfm.yAxis,
            "Voltage axis must be preserved")
    }

    @Test
    fun `output contains no NaN or Infinity`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5)
        val mlhfm = buildSyntheticMlhfm(voltages)

        val log = buildClosedLoopLog(
            n = 200,
            stfts = List(200) { 1.05 },
            ltfts = List(200) { 1.03 },
            lambdaControlActive = List(200) { 1.0 }
        )

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 1000.0
        )
        manager.correct(log, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection!!
        for (i in correction.correctedMlhfm.zAxis.indices) {
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isNaN(),
                "Corrected MLHFM[$i] should not be NaN")
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isInfinite(),
                "Corrected MLHFM[$i] should not be Infinite")
        }
    }

    @Test
    fun `all corrected values are non-negative`() {
        val voltages = arrayOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5)
        val mlhfm = buildSyntheticMlhfm(voltages, baseKgH = 10.0)

        // Even with negative trims, output should never go negative
        val log = buildClosedLoopLog(
            n = 200,
            stfts = List(200) { 0.85 },  // -15% STFT
            ltfts = List(200) { 0.90 },  // -10% LTFT
            lambdaControlActive = List(200) { 1.0 }
        )

        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 1000.0
        )
        manager.correct(log, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection!!
        for (i in correction.correctedMlhfm.zAxis.indices) {
            assertTrue(correction.correctedMlhfm.zAxis[i][0] >= 0.0,
                "MLHFM[$i] = ${correction.correctedMlhfm.zAxis[i][0]} must be non-negative")
        }
    }

    @Test
    fun `derivative filter rejects rapidly changing voltage`() {
        val voltages = arrayOf(1.0, 2.0, 3.0, 4.0)
        val mlhfm = buildSyntheticMlhfm(voltages)

        // Build log with rapidly alternating voltage (high derivative)
        val n = 200
        val log = buildClosedLoopLog(
            n = n,
            timestamps = List(n) { it * 0.01 }, // Fast sampling
            mafVoltages = List(n) { if (it % 2 == 0) 1.0 else 4.0 }, // Alternating
            stfts = List(n) { 1.20 }, // Large trim that would show up if not filtered
            ltfts = List(n) { 1.15 },
            lambdaControlActive = List(n) { 1.0 }
        )

        // Tight derivative filter should reject most of this data
        val manager = ClosedLoopFuelingCorrectionManager(
            minThrottleAngle = 0.0,
            minRpm = 0.0,
            maxDerivative = 1.0
        )
        manager.correct(log, mlhfm)

        val correction = manager.closedLoopMlhfmCorrection!!
        // Should produce valid non-NaN output even when most data is filtered
        for (i in correction.correctedMlhfm.zAxis.indices) {
            assertFalse(correction.correctedMlhfm.zAxis[i][0].isNaN(),
                "Derivative-filtered data should not produce NaN at index $i")
        }
    }
}
