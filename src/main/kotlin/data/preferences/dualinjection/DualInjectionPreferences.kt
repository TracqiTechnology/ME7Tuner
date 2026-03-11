package data.preferences.dualinjection

import java.util.prefs.Preferences

object DualInjectionPreferences {
    private val prefs = Preferences.userNodeForPackage(DualInjectionPreferences::class.java)

    var portInjectorFlowRateCcMin: Double
        get() = prefs.get("port_flow_rate", "220.0").toDouble()
        set(value) = prefs.put("port_flow_rate", value.toString())

    var portInjectorFuelPressureBar: Double
        get() = prefs.get("port_fuel_pressure", "4.0").toDouble()
        set(value) = prefs.put("port_fuel_pressure", value.toString())

    var portInjectorDeadTimeMs: Double
        get() = prefs.get("port_dead_time", "0.0").toDouble()
        set(value) = prefs.put("port_dead_time", value.toString())

    var directInjectorFlowRateCcMin: Double
        get() = prefs.get("direct_flow_rate", "160.0").toDouble()
        set(value) = prefs.put("direct_flow_rate", value.toString())

    var directInjectorFuelPressureBar: Double
        get() = prefs.get("direct_fuel_pressure", "200.0").toDouble()
        set(value) = prefs.put("direct_fuel_pressure", value.toString())

    var portSharePercentDefault: Double
        get() = prefs.get("port_share_percent", "30.0").toDouble()
        set(value) = prefs.put("port_share_percent", value.toString())

    var numPortInjectors: Int
        get() = prefs.get("num_port_injectors", "5").toInt()
        set(value) = prefs.put("num_port_injectors", value.toString())

    var numDirectInjectors: Int
        get() = prefs.get("num_direct_injectors", "5").toInt()
        set(value) = prefs.put("num_direct_injectors", value.toString())
}
