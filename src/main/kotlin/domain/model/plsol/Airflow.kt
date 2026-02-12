package domain.model.plsol

import domain.math.Point
import domain.model.load.EngineLoad

class Airflow(loadPoints: Array<Point>, displacement: Double, rpm: Int) {
    val points: Array<Point> = Array(loadPoints.size) { i ->
        Point(loadPoints[i].x, EngineLoad.getAirflow(loadPoints[i].x * 0.01, rpm.toDouble(), displacement))
    }
}
