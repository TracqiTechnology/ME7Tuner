package data.parser.me7log

import data.contract.Me7LogFileContract
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

object ClosedLoopLogParser {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _logs = MutableSharedFlow<Map<Me7LogFileContract.Header, List<Double>>>(extraBufferCapacity = 1)
    val logs: SharedFlow<Map<Me7LogFileContract.Header, List<Double>>> = _logs.asSharedFlow()

    fun loadDirectory(directory: File) {
        if (directory.isDirectory) {
            scope.launch {
                try {
                    val parser = Me7LogParser()
                    val logMap = parser.parseLogDirectory(Me7LogParser.LogType.CLOSED_LOOP, directory) { _, _ -> }
                    _logs.emit(logMap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
