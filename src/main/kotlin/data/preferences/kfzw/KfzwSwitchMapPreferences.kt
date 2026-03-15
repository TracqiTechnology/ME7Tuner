package data.preferences.kfzw

import data.parser.bin.BinParser
import data.parser.xdf.TableDefinition
import data.preferences.MapPreference
import domain.math.map.Map3d
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.prefs.Preferences

object KfzwSwitchMapPreferences {
    private const val COUNT_KEY = "kfzw_switch_count"
    private val prefs = Preferences.userNodeForPackage(KfzwSwitchMapPreferences::class.java)

    private val _switchMapsChanged = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val switchMapsChanged: SharedFlow<Unit> = _switchMapsChanged.asSharedFlow()

    private val slots = mutableMapOf<Int, MapPreference>()

    var count: Int
        get() = prefs.getInt(COUNT_KEY, 0)
        private set(value) { prefs.putInt(COUNT_KEY, value) }

    fun getSlot(index: Int): MapPreference {
        return slots.getOrPut(index) {
            MapPreference(
                "kfzw_switch_${index}_title",
                "kfzw_switch_${index}_description",
                "kfzw_switch_${index}_unit"
            )
        }
    }

    fun addMap(tableDefinition: TableDefinition) {
        val idx = count
        getSlot(idx).setSelectedMap(tableDefinition)
        count = idx + 1
        _switchMapsChanged.tryEmit(Unit)
    }

    fun removeMap(index: Int) {
        val n = count
        if (index < 0 || index >= n) return

        // Shift all slots above index down by one
        for (i in index until n - 1) {
            val nextSelected = getSlot(i + 1).getSelectedMap()
            if (nextSelected != null) {
                getSlot(i).setSelectedMap(nextSelected.first)
            } else {
                getSlot(i).clear()
            }
        }
        // Clear the last slot
        getSlot(n - 1).clear()
        slots.remove(n - 1)
        count = n - 1
        _switchMapsChanged.tryEmit(Unit)
    }

    fun getAllSelectedMaps(): List<Pair<Int, Pair<TableDefinition, Map3d>?>> {
        val n = count
        return (0 until n).map { i -> i to getSlot(i).getSelectedMap() }
    }

    fun clear() {
        val n = count
        for (i in 0 until n) {
            getSlot(i).clear()
            slots.remove(i)
        }
        count = 0
        _switchMapsChanged.tryEmit(Unit)
    }
}
