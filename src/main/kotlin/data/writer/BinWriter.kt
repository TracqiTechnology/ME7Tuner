package data.writer

import data.parser.xdf.TableDefinition
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

    fun write(file: File, tableDefinition: TableDefinition, map: Map3d) {
        RandomAccessFile(file, "rws").use { raf ->
            if (tableDefinition.xAxis != null && tableDefinition.xAxis.address != INVALID_ADDRESS) {
                val xAxis = DoubleArray(maxOf(tableDefinition.xAxis.rowCount, 1) * maxOf(tableDefinition.xAxis.indexCount, 1))
                for (i in map.xAxis.indices) xAxis[i] = map.xAxis[i]
                write(raf, tableDefinition.xAxis.address, tableDefinition.xAxis.sizeBits, tableDefinition.xAxis.equation, xAxis)
            }

            if (tableDefinition.yAxis != null && tableDefinition.yAxis.address != INVALID_ADDRESS) {
                val yAxis = DoubleArray(maxOf(tableDefinition.yAxis.rowCount, 1) * maxOf(tableDefinition.yAxis.indexCount, 1))
                for (i in map.yAxis.indices) yAxis[i] = map.yAxis[i]
                write(raf, tableDefinition.yAxis.address, tableDefinition.yAxis.sizeBits, tableDefinition.yAxis.equation, yAxis)
            }

            if (tableDefinition.zAxis.address != INVALID_ADDRESS) {
                val zAxis = DoubleArray(maxOf(tableDefinition.zAxis.rowCount, 1) * maxOf(tableDefinition.zAxis.columnCount, 1))
                var index = 0
                for (i in map.zAxis.indices) {
                    for (j in map.zAxis[i].indices) {
                        zAxis[index++] = map.zAxis[i][j]
                    }
                }
                write(raf, tableDefinition.zAxis.address, tableDefinition.zAxis.sizeBits, tableDefinition.zAxis.equation, zAxis)
            }
        }

        _writeEvents.tryEmit(tableDefinition)
    }

    private fun write(raf: RandomAccessFile, address: Int, size: Int, equation: String, values: DoubleArray) {
        try {
            val compiledScript = (engine as Compilable).compile("function func(X) { return ${inverse(equation)} }")
            compiledScript.eval(compiledScript.engine.getBindings(ScriptContext.ENGINE_SCOPE))
            val funcEngine = compiledScript.engine as Invocable

            raf.seek(address.toLong())
            val bb = ByteBuffer.allocate(values.size * (size / 8)).order(ByteOrder.LITTLE_ENDIAN)
            for (value in values) {
                if (size == 8) bb.put((funcEngine.invokeFunction("func", value) as Number).toByte())
                else if (size == 16) bb.putShort((funcEngine.invokeFunction("func", value) as Number).toShort())
            }
            raf.write(bb.array())
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun inverse(equation: String): String {
        val operators = extractOperators(equation)
        val operands = extractOperands(equation)

        if (operators.isEmpty() && operands.isEmpty()) return equation

        val hasMultiply = operators.isNotEmpty() && operators[0] == "*"
        val scale = if (hasMultiply && operands.isNotEmpty()) operands[0] else null
        val offset = when {
            operators.size > 1 && operands.size > 1 -> operands[1]
            hasMultiply && operands.size > 1 -> operands[1]
            !hasMultiply && operands.isNotEmpty() -> operands[0]
            else -> null
        }

        return when {
            scale != null && offset != null -> "(X - $offset) / $scale"
            scale != null -> "X / $scale"
            offset != null -> "X - $offset"
            else -> equation
        }
    }

    private fun extractOperators(equation: String): List<String> {
        val regex = Regex("[*+]")
        return regex.findAll(equation).map { it.value }.toList()
    }

    private fun extractOperands(equation: String): List<Double> {
        val regex = Regex("(\\+|-)?([0-9]*\\.?[0-9]+)")
        return regex.findAll(equation).map { it.value.toDouble() }.toList()
    }
}
