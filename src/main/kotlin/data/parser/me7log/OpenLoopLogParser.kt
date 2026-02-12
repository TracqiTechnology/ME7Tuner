package data.parser.me7log

import data.contract.Me7LogFileContract
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

object OpenLoopLogParser {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _logs = MutableSharedFlow<Map<Me7LogFileContract.Header, List<Double>>>(extraBufferCapacity = 1)
    val logs: SharedFlow<Map<Me7LogFileContract.Header, List<Double>>> = _logs.asSharedFlow()

    fun loadFile(file: File) {
        scope.launch {
            try {
                val parser = Me7LogParser()
                val logMap = parser.parseLogFile(Me7LogParser.LogType.OPEN_LOOP, file)
                _logs.emit(logMap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
