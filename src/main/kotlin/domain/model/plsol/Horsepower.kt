package domain.model.plsol

import domain.math.Point

class Horsepower(airflowPoints: Array<Point>) {
    val points: Array<Point> = Array(airflowPoints.size) { i ->
        Point(airflowPoints[i].x, airflowPoints[i].y / 0.75)
    }
}
