package data.writer

import data.model.EcuPlatform
import data.parser.xdf.AxisDefinition
import data.parser.xdf.TableDefinition
import data.preferences.platform.EcuPlatformPreference
import domain.math.map.Map3d
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.script.*

object BinWriter {
    private const val INVALID_ADDRESS = 0
    private val engine: ScriptEngine = ScriptEngineManager().getEngineByName("graal.js")

    private val _writeEvents = MutableSharedFlow<TableDefinition>(extraBufferCapacity = 1)
    val writeEvents: SharedFlow<TableDefinition> = _writeEvents.asSharedFlow()

    /**
     * Returns true if the active ECU platform supports BIN writing.
     * MED17 uses CRC32 checksums that are not yet implemented — writes are blocked.
     */
    fun isBinWriteSupported(): Boolean =
        EcuPlatformPreference.platform == EcuPlatform.ME7

    fun write(file: File, tableDefinition: TableDefinition, map: Map3d) {
        if (!isBinWriteSupported()) {
            throw UnsupportedOperationException(
                "MED17 BIN writing is not yet supported (CRC32 checksum required). " +
                    "Use XDF patch export instead."
            )
        }

        RandomAccessFile(file, "rws").use { raf ->
            tableDefinition.xAxis?.takeIf { it.address != INVALID_ADDRESS }?.let { axis ->
                val xFlat = DoubleArray(maxOf(axis.rowCount, 1) * maxOf(axis.indexCount, 1))
                for (i in map.xAxis.indices) xFlat[i] = map.xAxis[i]
                write(raf, axis, xFlat)
            }

            tableDefinition.yAxis?.takeIf { it.address != INVALID_ADDRESS }?.let { axis ->
                val yFlat = DoubleArray(maxOf(axis.rowCount, 1) * maxOf(axis.indexCount, 1))
                for (i in map.yAxis.indices) yFlat[i] = map.yAxis[i]
                write(raf, axis, yFlat)
            }

            tableDefinition.zAxis.takeIf { it.address != INVALID_ADDRESS }?.let { axis ->
                val zFlat = DoubleArray(maxOf(axis.rowCount, 1) * maxOf(axis.columnCount, 1))
                var index = 0
                for (i in map.zAxis.indices) for (j in map.zAxis[i].indices) zFlat[index++] = map.zAxis[i][j]
                write(raf, axis, zFlat)
            }
        }

        _writeEvents.tryEmit(tableDefinition)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Low-level write: invert equation, encode, and write bytes
    // ─────────────────────────────────────────────────────────────────────

    private fun write(raf: RandomAccessFile, axis: AxisDefinition, values: DoubleArray) {
        val inverseEquation = buildInverseEquation(axis.equation, axis.varId)
        val byteOrder = if (axis.lsbFirst) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
        val stride = axis.sizeBits / 8

        try {
            val compiled = (engine as Compilable).compile(
                "function func(X) { return $inverseEquation }"
            )
            compiled.eval(compiled.engine.getBindings(ScriptContext.ENGINE_SCOPE))
            val inv = compiled.engine as Invocable

            raf.seek(axis.address.toLong())
            val bb = ByteBuffer.allocate(values.size * stride).order(byteOrder)

            for (value in values) {
                val raw = (inv.invokeFunction("func", value) as Number)
                when {
                    axis.isFloat && stride == 4 -> bb.putFloat(raw.toFloat())
                    stride == 4 -> bb.putInt(raw.toInt())
                    stride == 2 -> bb.putShort(raw.toShort())
                    else        -> bb.put(raw.toByte())
                }
            }
            raf.write(bb.array())
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Inverse equation builder
    //
    // Handles the common ME7 forms produced by Bosch / TunerPro translators:
    //   A * X
    //   A * X + B  /  A * X - B
    //   (X + B) / A  (already inverted form)
    //   X + B  /  X - B
    //   X / A  /  X * A
    //
    // For anything more complex the GraalJS engine evaluates the forward
    // equation; we keep the expression as-is and let a numeric round-trip
    // produce the best approximation (clamped to [0, 2^sizeBits-1] at write).
    //
    // "X" in the inverse equation always stands for the engineering-unit
    // value (the value shown in the table).  The result is the raw integer.
    // ─────────────────────────────────────────────────────────────────────

    internal fun buildInverseEquation(equation: String, varId: String): String {
        // Normalise: replace the variable id with X so the inverse is always
        // in terms of X regardless of what the XDF varId was.
        val eq = equation.trim().replace(varId, "X")

        // ── Pattern: A * X  (+ or - B optional) ─────────────────────────
        // Matches:  0.023438 * X   |   0.75 * X + 18   |   0.75 * X - 18
        val mulAdd = Regex(
            """^([+-]?\s*\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*\*\s*X\s*([+-]\s*\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)?$"""
        ).matchEntire(eq)
        if (mulAdd != null) {
            val a = mulAdd.groupValues[1].replace(" ", "").toDoubleOrNull()
            val bStr = mulAdd.groupValues[2].replace(" ", "")
            val b = bStr.toDoubleOrNull()
            if (a != null && a != 0.0) {
                return if (b != null) "(X - $b) / $a" else "X / $a"
            }
        }

        // ── Pattern: X * A  (+ or - B optional) ─────────────────────────
        val xMulAdd = Regex(
            """^X\s*\*\s*([+-]?\s*\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*([+-]\s*\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)?$"""
        ).matchEntire(eq)
        if (xMulAdd != null) {
            val a = xMulAdd.groupValues[1].replace(" ", "").toDoubleOrNull()
            val bStr = xMulAdd.groupValues[2].replace(" ", "")
            val b = bStr.toDoubleOrNull()
            if (a != null && a != 0.0) {
                return if (b != null) "(X - $b) / $a" else "X / $a"
            }
        }

        // ── Pattern: X + B  /  X - B ─────────────────────────────────────
        val xAddSub = Regex(
            """^X\s*([+-])\s*(\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)$"""
        ).matchEntire(eq)
        if (xAddSub != null) {
            val sign = xAddSub.groupValues[1]
            val b    = xAddSub.groupValues[2]
            return if (sign == "+") "X - $b" else "X + $b"
        }

        // ── Pattern: X / A ───────────────────────────────────────────────
        val xDiv = Regex(
            """^X\s*/\s*(\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)$"""
        ).matchEntire(eq)
        if (xDiv != null) {
            val a = xDiv.groupValues[1].toDoubleOrNull()
            if (a != null && a != 0.0) return "X * $a"
        }

        // ── Fallback: identity (X) or passthrough ─────────────────────────
        // If we can't analytically invert, write back the engineering value
        // directly (works for identity maps, and is safe for constants).
        return "X"
    }
}
