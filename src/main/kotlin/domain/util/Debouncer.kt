package domain.util

import kotlinx.coroutines.*

class Debouncer(private val scope: CoroutineScope) {
    private val jobs = mutableMapOf<Any, Job>()

    fun debounce(key: Any, delayMs: Long, action: suspend () -> Unit) {
        jobs[key]?.cancel()
        jobs[key] = scope.launch {
            delay(delayMs)
            try {
                action()
            } finally {
                jobs.remove(key)
            }
        }
    }

    fun shutdown() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}
