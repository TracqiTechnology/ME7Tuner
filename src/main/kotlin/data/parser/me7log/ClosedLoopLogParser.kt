package data.parser.me7log

import data.contract.Me7LogFileContract
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

data class LogLoadProgress(val loaded: Int, val total: Int)

object ClosedLoopLogParser {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _logs = MutableSharedFlow<Map<Me7LogFileContract.Header, List<Double>>>(extraBufferCapacity = 1)
    val logs: SharedFlow<Map<Me7LogFileContract.Header, List<Double>>> = _logs.asSharedFlow()

    private val _progress = MutableStateFlow<LogLoadProgress?>(null)
    val progress: StateFlow<LogLoadProgress?> = _progress.asStateFlow()

    fun loadDirectory(directory: File) {
        if (directory.isDirectory) {
            scope.launch {
                try {
                    _progress.value = LogLoadProgress(0, 0)
                    val parser = Me7LogParser()
                    val logMap = parser.parseLogDirectory(Me7LogParser.LogType.CLOSED_LOOP, directory) { loaded, total ->
                        _progress.value = LogLoadProgress(loaded, total)
                    }
                    _logs.emit(logMap)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _progress.value = null
                }
            }
        }
    }
}
