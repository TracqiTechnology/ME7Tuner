package ui.navigation

import data.model.EcuPlatform

enum class CalibrationTab(
    val label: String,
    val tooltip: String,
    val platforms: Set<EcuPlatform> = setOf(EcuPlatform.ME7, EcuPlatform.MED17),
    val med17Label: String? = null
) {
    FUELING("Fueling", "KRKTE Calculator & Injector Scaling"),
    CLOSED_LOOP("Closed Loop", "Closed Loop MLHFM Compensation", platforms = setOf(EcuPlatform.ME7)),
    OPEN_LOOP("Open Loop", "Open Loop MLHFM Compensation", platforms = setOf(EcuPlatform.ME7)),
    DUAL_INJECTION("Dual Injection", "Port + Direct Injector Split Calculator", platforms = setOf(EcuPlatform.MED17)),
    FUEL_TRIM("Fuel Trim", "STFT/LTFT Analysis & rk_w Corrections", platforms = setOf(EcuPlatform.MED17)),
    PLSOL("PLSOL", "Requested Boost"),
    KFMIOP("KFMIOP", "KFMIOP Calculator", med17Label = "KFLMIOP"),
    KFMIRL("KFMIRL", "KFMIRL Calculator", med17Label = "KFLMIRL"),
    KFZWOP("KFZWOP", "KFZWOP Calculator"),
    KFZW("KFZW", "KFZW Calculator"),
    KFVPDKSD("KFVPDKSD/E", "KFVPDKSD/E Calculator", platforms = setOf(EcuPlatform.ME7)),
    WDKUGDN("WDKUGDN", "KFURL", platforms = setOf(EcuPlatform.ME7)),
    LDRPID("LDRPID", "LDRPID");

    /** Returns the display label appropriate for the active platform. */
    fun labelFor(platform: EcuPlatform): String =
        if (platform == EcuPlatform.MED17 && med17Label != null) med17Label else label
}
