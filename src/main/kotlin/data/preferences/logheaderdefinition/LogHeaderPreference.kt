package data.preferences.logheaderdefinition

import data.contract.Me7LogFileContract
import java.util.prefs.Preferences

object LogHeaderPreference {
    private val prefs = Preferences.userNodeForPackage(LogHeaderPreference::class.java)

    fun getHeader(header: Me7LogFileContract.Header): String {
        return prefs.get(header.name, header.header)
    }

    fun setHeader(header: Me7LogFileContract.Header, value: String) {
        prefs.put(header.name, value)
        header.header = value
    }

    fun loadHeaders() {
        for (header in Me7LogFileContract.Header.entries) {
            header.header = prefs.get(header.name, header.header)
        }
    }
}
