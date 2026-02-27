package ui.navigation

enum class CalibrationTab(val label: String, val tooltip: String) {
    FUELING("Fueling", "KRKTE Calculator & Injector Scaling"),
    CLOSED_LOOP("Closed Loop", "Closed Loop MLHFM Compensation"),
    OPEN_LOOP("Open Loop", "Open Loop MLHFM Compensation"),
    PLSOL("PLSOL", "Requested Boost"),
    KFMIOP("KFMIOP", "KFMIOP Calculator"),
    KFMIRL("KFMIRL", "KFMIRL Calculator"),
    KFZWOP("KFZWOP", "KFZWOP Calculator"),
    KFZW("KFZW", "KFZW Calculator"),
    KFVPDKSD("KFVPDKSD/E", "KFVPDKSD/E Calculator"),
    WDKUGDN("WDKUGDN", "KFURL"),
    LDRPID("LDRPID", "LDRPID")
}
