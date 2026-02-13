package data.profile

import data.contract.Me7LogFileContract
import data.parser.xdf.XdfParser
import data.preferences.closedloopfueling.ClosedLoopFuelingLogPreferences
import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import data.preferences.kfvpdksd.KfvpdksdPreferences
import data.preferences.kfwdkmsn.KfwdkmsnPreferences
import data.preferences.kfzw.KfzwPreferences
import data.preferences.kfzwop.KfzwopPreferences
import data.preferences.krkte.KrktePreferences
import data.preferences.logheaderdefinition.LogHeaderPreference
import data.preferences.MapPreference
import data.preferences.mlhfm.MlhfmPreferences
import data.preferences.openloopfueling.OpenLoopFuelingLogFilterPreferences
import data.preferences.plsol.PlsolPreferences
import data.preferences.primaryfueling.PrimaryFuelingPreferences
import data.preferences.wdkugdn.WdkugdnPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

object ProfileManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _defaultProfiles = MutableStateFlow<List<ConfigurationProfile>>(emptyList())
    val defaultProfiles: StateFlow<List<ConfigurationProfile>> = _defaultProfiles.asStateFlow()

    private val _userProfiles = MutableStateFlow<List<ConfigurationProfile>>(emptyList())
    val userProfiles: StateFlow<List<ConfigurationProfile>> = _userProfiles.asStateFlow()

    private val mapPreferencesByKey: Map<String, MapPreference> = mapOf(
        "KRKTE" to KrktePreferences,
        "MLHFM" to MlhfmPreferences,
        "KFMIOP" to KfmiopPreferences,
        "KFMIRL" to KfmirlPreferences,
        "KFZWOP" to KfzwopPreferences,
        "KFZW" to KfzwPreferences,
        "KFVPDKSD" to KfvpdksdPreferences,
        "WDKUGDN" to WdkugdnPreferences,
        "KFWDKMSN" to KfwdkmsnPreferences,
        "KFLDRL" to KfldrlPreferences,
        "KFLDIMX" to KfldimxPreferences,
    )

    init {
        _defaultProfiles.value = loadBundledProfiles()
    }

    fun exportCurrentProfile(name: String): ConfigurationProfile {
        val mapDefs = mutableMapOf<String, MapDefinitionRef>()
        for ((key, pref) in mapPreferencesByKey) {
            val selected = pref.getSelectedMap()
            if (selected != null) {
                val def = selected.first
                mapDefs[key] = MapDefinitionRef(
                    tableName = def.tableName,
                    tableDescription = def.tableDescription,
                    unit = def.zAxis.unit
                )
            }
        }

        val logHeaders = mutableMapOf<String, String>()
        for (header in Me7LogFileContract.Header.entries) {
            logHeaders[header.name] = LogHeaderPreference.getHeader(header)
        }

        return ConfigurationProfile(
            name = name,
            mapDefinitions = mapDefs,
            primaryFueling = PrimaryFuelingConfig(
                airDensity = PrimaryFuelingPreferences.airDensity,
                displacement = PrimaryFuelingPreferences.displacement,
                numCylinders = PrimaryFuelingPreferences.numCylinders,
                stoichiometricAfr = PrimaryFuelingPreferences.stoichiometricAfr,
                gasolineGramsPerCcm = PrimaryFuelingPreferences.gasolineGramsPerCubicCentimeter,
                fuelInjectorSize = PrimaryFuelingPreferences.fuelInjectorSize
            ),
            plsol = PlsolConfig(
                barometricPressure = PlsolPreferences.barometricPressure,
                intakeAirTemperature = PlsolPreferences.intakeAirTemperature,
                kfurl = PlsolPreferences.kfurl,
                displacement = PlsolPreferences.displacement,
                rpm = PlsolPreferences.rpm
            ),
            kfmiop = KfmiopConfig(
                maxMapPressure = KfmiopPreferences.maxMapPressure,
                maxBoostPressure = KfmiopPreferences.maxBoostPressure
            ),
            kfvpdksd = KfvpdksdConfig(
                maxWastegateCrackingPressure = KfvpdksdPreferences.maxWastegateCrackingPressure
            ),
            wdkugdn = WdkugdnConfig(
                displacement = WdkugdnPreferences.displacement
            ),
            closedLoopFueling = ClosedLoopFuelingConfig(
                minThrottleAngle = ClosedLoopFuelingLogPreferences.minThrottleAngle,
                minRpm = ClosedLoopFuelingLogPreferences.minRpm,
                maxDerivative = ClosedLoopFuelingLogPreferences.maxDerivative
            ),
            openLoopFueling = OpenLoopFuelingConfig(
                minThrottleAngle = OpenLoopFuelingLogFilterPreferences.minThrottleAngle,
                minRpm = OpenLoopFuelingLogFilterPreferences.minRpm,
                minMe7Points = OpenLoopFuelingLogFilterPreferences.minMe7Points,
                minAfrPoints = OpenLoopFuelingLogFilterPreferences.minAfrPoints,
                maxAfr = OpenLoopFuelingLogFilterPreferences.maxAfr,
                fuelInjectorSize = OpenLoopFuelingLogFilterPreferences.fuelInjectorSize,
                gasolineGramsPerCcm = OpenLoopFuelingLogFilterPreferences.gasolineGramsPerCubicCentimeter,
                numFuelInjectors = OpenLoopFuelingLogFilterPreferences.numFuelInjectors
            ),
            logHeaders = logHeaders
        )
    }

    fun applyProfile(profile: ConfigurationProfile) {
        // Apply map definitions
        val tableDefs = XdfParser.tableDefinitions.value
        for ((key, ref) in profile.mapDefinitions) {
            val pref = mapPreferencesByKey[key] ?: continue
            val match = tableDefs.firstOrNull { def ->
                ref.tableName == def.tableName &&
                    ref.tableDescription == def.tableDescription &&
                    ref.unit == def.zAxis.unit
            }
            pref.setSelectedMap(match)
        }

        // Apply primary fueling
        profile.primaryFueling.let {
            PrimaryFuelingPreferences.airDensity = it.airDensity
            PrimaryFuelingPreferences.displacement = it.displacement
            PrimaryFuelingPreferences.numCylinders = it.numCylinders
            PrimaryFuelingPreferences.stoichiometricAfr = it.stoichiometricAfr
            PrimaryFuelingPreferences.gasolineGramsPerCubicCentimeter = it.gasolineGramsPerCcm
            PrimaryFuelingPreferences.fuelInjectorSize = it.fuelInjectorSize
        }

        // Apply PLSOL
        profile.plsol.let {
            PlsolPreferences.barometricPressure = it.barometricPressure
            PlsolPreferences.intakeAirTemperature = it.intakeAirTemperature
            PlsolPreferences.kfurl = it.kfurl
            PlsolPreferences.displacement = it.displacement
            PlsolPreferences.rpm = it.rpm
        }

        // Apply KFMIOP
        profile.kfmiop.let {
            KfmiopPreferences.maxMapPressure = it.maxMapPressure
            KfmiopPreferences.maxBoostPressure = it.maxBoostPressure
        }

        // Apply KFVPDKSD
        KfvpdksdPreferences.maxWastegateCrackingPressure = profile.kfvpdksd.maxWastegateCrackingPressure

        // Apply WDKUGDN
        WdkugdnPreferences.displacement = profile.wdkugdn.displacement

        // Apply closed loop fueling
        profile.closedLoopFueling.let {
            ClosedLoopFuelingLogPreferences.minThrottleAngle = it.minThrottleAngle
            ClosedLoopFuelingLogPreferences.minRpm = it.minRpm
            ClosedLoopFuelingLogPreferences.maxDerivative = it.maxDerivative
        }

        // Apply open loop fueling
        profile.openLoopFueling.let {
            OpenLoopFuelingLogFilterPreferences.minThrottleAngle = it.minThrottleAngle
            OpenLoopFuelingLogFilterPreferences.minRpm = it.minRpm
            OpenLoopFuelingLogFilterPreferences.minMe7Points = it.minMe7Points
            OpenLoopFuelingLogFilterPreferences.minAfrPoints = it.minAfrPoints
            OpenLoopFuelingLogFilterPreferences.maxAfr = it.maxAfr
            OpenLoopFuelingLogFilterPreferences.fuelInjectorSize = it.fuelInjectorSize
            OpenLoopFuelingLogFilterPreferences.gasolineGramsPerCubicCentimeter = it.gasolineGramsPerCcm
            OpenLoopFuelingLogFilterPreferences.numFuelInjectors = it.numFuelInjectors
        }

        // Apply log headers
        for ((headerName, value) in profile.logHeaders) {
            val header = Me7LogFileContract.Header.entries.firstOrNull { it.name == headerName } ?: continue
            LogHeaderPreference.setHeader(header, value)
        }
    }

    fun saveToFile(profile: ConfigurationProfile, file: File) {
        val jsonString = json.encodeToString(ConfigurationProfile.serializer(), profile)
        file.writeText(jsonString)
    }

    fun loadFromFile(file: File): ConfigurationProfile {
        val jsonString = file.readText()
        return json.decodeFromString(ConfigurationProfile.serializer(), jsonString)
    }

    fun loadBundledProfiles(): List<ConfigurationProfile> {
        val index = ProfileManager::class.java.getResourceAsStream("/profiles/index.txt")
            ?.bufferedReader()?.readLines() ?: return emptyList()
        return index
            .filter { it.isNotBlank() && it.endsWith(".me7profile.json") }
            .mapNotNull { fileName ->
                ProfileManager::class.java.getResourceAsStream("/profiles/$fileName")?.let { stream ->
                    runCatching {
                        json.decodeFromString(ConfigurationProfile.serializer(), stream.bufferedReader().readText())
                    }.getOrNull()
                }
            }
    }

    fun addUserProfile(profile: ConfigurationProfile) {
        _userProfiles.value = _userProfiles.value + profile
    }
}
