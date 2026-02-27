package domain.model.load

object EngineLoad {
    fun getAbsoluteEngineLoad(airflow: Double, rpm: Double, displacement: Double): Double {
        val airMass = airflow / (rpm / 60.0)
        return airMass / (1.184 * displacement) * 2
    }

    fun getAirflow(engineLoad: Double, rpm: Double, displacement: Double): Double {
        return (engineLoad * (1.184 * displacement) / 2) * (rpm / 60.0)
    }
}
