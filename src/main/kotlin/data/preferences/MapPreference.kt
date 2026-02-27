package data.preferences

import data.parser.bin.BinParser
import data.parser.xdf.TableDefinition
import domain.math.map.Map3d
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.prefs.Preferences

open class MapPreference(
    private val tableTitlePreference: String,
    private val tableDescriptionPreference: String,
    private val tableUnitPreference: String
) {
    private val prefs = Preferences.userNodeForPackage(MapPreference::class.java)

    private val _mapChanged = MutableSharedFlow<Pair<TableDefinition, Map3d>?>(extraBufferCapacity = 1)
    val mapChanged: SharedFlow<Pair<TableDefinition, Map3d>?> = _mapChanged.asSharedFlow()

    init { MapPreferenceManager.add(this) }

    fun clear() {
        runCatching { prefs.clear() }
        _mapChanged.tryEmit(null)
    }

    fun getSelectedMap(): Pair<TableDefinition, Map3d>? {
        val mapList = BinParser.mapList.value
        val mapTitle = prefs.get(tableTitlePreference, "")
        val mapDescription = prefs.get(tableDescriptionPreference, "")
        val mapUnit = prefs.get(tableUnitPreference, "")

        if (mapTitle.isEmpty() && mapDescription.isEmpty()) return null

        return mapList.firstOrNull { (def, _) ->
            mapTitle == def.tableName && mapDescription == def.tableDescription && mapUnit == def.zAxis.unit
        }
    }

    fun setSelectedMap(tableDefinition: TableDefinition?) {
        if (tableDefinition != null) {
            prefs.put(tableTitlePreference, tableDefinition.tableName)
            prefs.put(tableDescriptionPreference, tableDefinition.tableDescription)
            prefs.put(tableUnitPreference, tableDefinition.zAxis.unit)
        } else {
            prefs.put(tableTitlePreference, "")
            prefs.put(tableDescriptionPreference, "")
            prefs.put(tableUnitPreference, "")
        }
        _mapChanged.tryEmit(getSelectedMap())
    }
}
