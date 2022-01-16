package model.plsol;

import java.awt.geom.Point2D;
import java.util.List;

public class Plsol {

    private final Point2D.Double[] points;

    /**
     * @param pu barometric pressure
     * @param tans intake air temperature
     */
    public Plsol(double pu, double tans, double kfurl) {
        points = new Point2D.Double[400];
        double ps = pu;

        for(int i = 0; i < points.length; i++) {
            double x = i;
            double y = plsol(pu, ps, tans, 96, kfurl, x);
            ps = y;

            points[i] = new Point2D.Double(x,y);
        }
    }

    public Plsol(double pu, double tans, double kfurl, List<Double> load) {
        points = new Point2D.Double[load.size()];
        double ps = pu;

        for(int i = 0; i < points.length; i++) {
            double x = load.get(i);
            double y = plsol(pu, ps, tans, 96, kfurl, x);
            ps = y;

            points[i] = new Point2D.Double(x,y);
        }
    }


    public Point2D.Double[] getPoints() {
        return points;
    }

    /**
     *
     * @param pu barometric pressure
     * @param ps intake manifold absolute pressure
     * @param tans intake air temperature
     * @param tmot coolant temperature
     * @param rlsol requested engine load
     * @return
     */
    public static double plsol(double pu, double ps, double tans, double tmot, double kfurl, double rlsol) {
        final double KFPRG = 70.0;
        final double FPBRKDS = 1.016; // VE
        final double KFFWTBR = 0.02;
        final double VPSSPLS = 1.016; // pressure drop over the throttle plate

        double fho = pu/1013.0;

        double pirg = fho*KFPRG;

        double pbr = ps * FPBRKDS;
        double psagr = 250; // ?

        double evtmod = tans + (tmot - tans) * KFFWTBR;
        double fwft = (tans+673.425)/731.334;
        double ftbr = 273.0/(evtmod+273.0)*fwft;

        double fupsrl = kfurl * ftbr; // correct for air temperature (pressure)

        double rfagr = Math.max(pbr-pirg, 0)*fupsrl* psagr/ps; // correct for residual cylinder pressure

        double pssol = (rlsol + rfagr)/fupsrl/FPBRKDS;

        return pssol/VPSSPLS;
    }
}
