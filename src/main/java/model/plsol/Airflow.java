package model.plsol;

import model.load.EngineLoad;

import java.awt.geom.Point2D;

public class Airflow {
    private final Point2D.Double[] points;

    /**
     * Get airflow for a load and displacement
     * @param loadPoints an RPM (x) and a load (y)
     * @param displacement engine displacement in liters
     */
    public Airflow(Point2D.Double[] loadPoints, double displacement, int rpm) {
        points = new Point2D.Double[loadPoints.length];

        for(int i = 0; i < points.length; i++) {
            points[i] = new Point2D.Double(loadPoints[i].x,EngineLoad.getAirflow(loadPoints[i].x * 0.01, rpm, displacement));
        }
    }

    public Point2D.Double[] getPoints() {
        return points;
    }
}
