package data.profile

import kotlinx.serialization.Serializable

@Serializable
data class ConfigurationProfile(
    val name: String = "",
    val mapDefinitions: Map<String, MapDefinitionRef> = emptyMap(),
    val primaryFueling: PrimaryFuelingConfig = PrimaryFuelingConfig(),
    val plsol: PlsolConfig = PlsolConfig(),
    val kfmiop: KfmiopConfig = KfmiopConfig(),
    val kfvpdksd: KfvpdksdConfig = KfvpdksdConfig(),
    val wdkugdn: WdkugdnConfig = WdkugdnConfig(),
    val closedLoopFueling: ClosedLoopFuelingConfig = ClosedLoopFuelingConfig(),
    val openLoopFueling: OpenLoopFuelingConfig = OpenLoopFuelingConfig(),
    val logHeaders: Map<String, String> = emptyMap()
)

@Serializable
data class MapDefinitionRef(
    val tableName: String = "",
    val tableDescription: String = "",
    val unit: String = ""
)

@Serializable
data class PrimaryFuelingConfig(
    val airDensity: Double = 1.2929,
    val displacement: Double = 0.4496,
    val numCylinders: Int = 6,
    val stoichiometricAfr: Double = 14.7,
    val gasolineGramsPerCcm: Double = 0.7,
    val fuelInjectorSize: Double = 349.0
)

@Serializable
data class PlsolConfig(
    val barometricPressure: Double = 1013.0,
    val intakeAirTemperature: Double = 20.0,
    val kfurl: Double = 0.1037,
    val displacement: Double = 2.7,
    val rpm: Int = 6000
)

@Serializable
data class KfmiopConfig(
    val maxMapPressure: Double = 2550.0,
    val maxBoostPressure: Double = 2100.0
)

@Serializable
data class KfvpdksdConfig(
    val maxWastegateCrackingPressure: Double = 200.0
)

@Serializable
data class WdkugdnConfig(
    val displacement: Double = 2.7
)

@Serializable
data class ClosedLoopFuelingConfig(
    val minThrottleAngle: Double = 0.0,
    val minRpm: Double = 0.0,
    val maxDerivative: Double = 50.0
)

@Serializable
data class OpenLoopFuelingConfig(
    val minThrottleAngle: Double = 80.0,
    val minRpm: Double = 2000.0,
    val minMe7Points: Int = 75,
    val minAfrPoints: Int = 150,
    val maxAfr: Double = 16.0,
    val fuelInjectorSize: Double = 349.0,
    val gasolineGramsPerCcm: Double = 0.7,
    val numFuelInjectors: Double = 6.0
)
