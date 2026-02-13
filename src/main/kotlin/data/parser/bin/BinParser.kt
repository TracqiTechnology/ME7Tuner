package data.parser.bin

import data.parser.xdf.AxisDefinition
import data.parser.xdf.TableDefinition
import data.parser.xdf.XdfParser
import data.preferences.bin.BinFilePreferences
import data.writer.BinWriter
import domain.math.map.Map3d
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.script.*

object BinParser {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val engine: ScriptEngine = ScriptEngineManager().getEngineByName("graal.js")
    private val parseMutex = Mutex()

    private val _mapList = MutableStateFlow<List<Pair<TableDefinition, Map3d>>>(emptyList())
    val mapList: StateFlow<List<Pair<TableDefinition, Map3d>>> = _mapList.asStateFlow()

    private var binaryFile = File("")

    fun init() {
        scope.launch {
            BinFilePreferences.file.collect { file ->
                binaryFile = file
                if (binaryFile.exists() && binaryFile.isFile) {
                    try {
                        parseMutex.withLock {
                            parse(BufferedInputStream(FileInputStream(binaryFile)), XdfParser.tableDefinitions.value)
                        }
                    } catch (e: IOException) { e.printStackTrace() }
                }
            }
        }
        scope.launch {
            XdfParser.tableDefinitions.collect { tableDefinitions ->
                if (binaryFile.exists() && binaryFile.isFile) {
                    try {
                        parseMutex.withLock {
                            parse(BufferedInputStream(FileInputStream(binaryFile)), tableDefinitions)
                        }
                    } catch (e: IOException) { e.printStackTrace() }
                }
            }
        }
        scope.launch {
            BinWriter.writeEvents.collect {
                if (binaryFile.exists() && binaryFile.isFile) {
                    try {
                        parseMutex.withLock {
                            parse(BufferedInputStream(FileInputStream(binaryFile)), XdfParser.tableDefinitions.value)
                        }
                    } catch (e: IOException) { e.printStackTrace() }
                }
            }
        }
    }

    private fun parse(inputStream: InputStream, tableDefinitions: List<TableDefinition>) {
        val result = mutableListOf<Pair<TableDefinition, Map3d>>()
        val bytes: ByteArray
        BufferedInputStream(inputStream).use { bytes = it.readAllBytes() }

        for (tableDefinition in tableDefinitions) {
            val xAxis = tableDefinition.xAxis?.let { parseAxis(bytes, it) } ?: emptyArray()
            val yAxis = tableDefinition.yAxis?.let { parseAxis(bytes, it) } ?: emptyArray()
            val zAxis = parseData(bytes, tableDefinition.zAxis)
            result.add(tableDefinition to Map3d(xAxis, yAxis, zAxis))
        }

        _mapList.value = result
    }

    private fun parseAxis(bytes: ByteArray, axisDefinition: AxisDefinition): Array<Double> {
        val buffer = ByteBuffer.wrap(bytes)
        val address = axisDefinition.address

        if (address != 0) {
            buffer.position(address)
            val strideBytes = axisDefinition.sizeBits / 8
            buffer.limit(address + strideBytes * axisDefinition.indexCount)
            val slice = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
            slice.position(0)

            try {
                val compiledScript = (engine as Compilable).compile("function func(${axisDefinition.varId}) { return ${axisDefinition.equation} }")
                compiledScript.eval(compiledScript.engine.getBindings(ScriptContext.ENGINE_SCOPE))
                val funcEngine = compiledScript.engine as Invocable

                return Array(axisDefinition.indexCount) { _ ->
                    val value = if (strideBytes == 1) {
                        if (axisDefinition.type % 2 == 0) java.lang.Byte.toUnsignedInt(slice.get()) else slice.get().toInt()
                    } else {
                        if (axisDefinition.type % 2 == 0) java.lang.Short.toUnsignedInt(slice.short) else slice.short.toInt()
                    }
                    (funcEngine.invokeFunction("func", value) as Number).toDouble()
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else if (axisDefinition.indexCount != 0) {
            return Array(axisDefinition.indexCount) { i -> axisDefinition.axisValues[i].second.toDouble() }
        }

        return emptyArray()
    }

    private fun parseData(bytes: ByteArray, axisDefinition: AxisDefinition): Array<Array<Double>> {
        val buffer = ByteBuffer.wrap(bytes)
        val address = axisDefinition.address

        if (address != 0) {
            buffer.position(address)
            val rowCount = axisDefinition.rowCount
            val columnCount = maxOf(1, axisDefinition.columnCount)
            val stride = axisDefinition.sizeBits / 8

            buffer.limit(address + stride * rowCount * columnCount)
            val slice = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
            slice.position(0)

            try {
                val compiledScript = (engine as Compilable).compile("function func(${axisDefinition.varId}) { return ${axisDefinition.equation} }")
                compiledScript.eval(compiledScript.engine.getBindings(ScriptContext.ENGINE_SCOPE))
                val funcEngine = compiledScript.engine as Invocable

                return Array(rowCount) { _ ->
                    Array(columnCount) { _ ->
                        val value = if (stride == 1) {
                            if (axisDefinition.type % 2 == 0) java.lang.Byte.toUnsignedInt(slice.get()) else slice.get().toInt()
                        } else {
                            if (axisDefinition.type % 2 == 0) java.lang.Short.toUnsignedInt(slice.short) else slice.short.toInt()
                        }
                        (funcEngine.invokeFunction("func", value) as Number).toDouble()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        return emptyArray()
    }
}
