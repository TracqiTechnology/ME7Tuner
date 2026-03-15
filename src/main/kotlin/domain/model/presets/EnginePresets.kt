package domain.model.presets

data class EnginePreset(val name: String, val displacementCc: Double, val cylinderCount: Int)

object EnginePresets {
    val EA855_EVO_2_5T = EnginePreset("EA855 EVO 2.5T (RS3/TTRS)", 2480.0, 5)
    val EA825_4_0T = EnginePreset("EA825 4.0T (RS6/RS7)", 3996.0, 8)
    val FSI_5_2_V10 = EnginePreset("5.2 V10 FSI (R8/Huracan)", 5204.0, 10)
    val EA113_1_8T = EnginePreset("EA113 1.8T (B5 S4)", 1781.0, 4)
    val EA835_2_7T = EnginePreset("EA835 2.7T (B5 S4/allroad)", 2671.0, 6)

    val all = listOf(EA855_EVO_2_5T, EA825_4_0T, FSI_5_2_V10, EA113_1_8T, EA835_2_7T)
}
