package model.plsol;

import java.awt.geom.Point2D;

public class Horsepower {

    private final Point2D.Double[] points;

    public Horsepower(Point2D.Double[] airflowPoints) {
        points = new Point2D.Double[airflowPoints.length];

        for(int i = 0; i < points.length; i++) {
            // Your peak airflow should be roughly 0.75 times your horsepower.
            points[i] = new Point2D.Double(airflowPoints[i].x, airflowPoints[i].y/0.75 );
        }
    }

    public Point2D.Double[] getPoints() {
        return points;
    }
}
