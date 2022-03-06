package domain.model.rlsol;

import java.awt.geom.Point2D;
import java.util.List;

public class Rlsol {

    private final Point2D.Double[] points;

    /**
     * @param pu barometric pressure
     * @param tans intake air temperature
     * @param kfurl ~0.1037
     * @param pressure pressures in mbar
     */
    public Rlsol(double pu, double tans, double kfurl, List<Double> pressure) {
        points = new Point2D.Double[pressure.size()];
        double ps = pressure.get(0);

        for(int i = 0; i < points.length; i++) {
            double x = pressure.get(i);
            double y = rlsol(pu, ps, tans, 96, kfurl, x);
            ps = x;

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
     * @param kfurl ~0.1037
     * @param plsol requested boost pressure
     * @return
     */
     public static double rlsol(double pu, double ps, double tans, double tmot, double kfurl, double plsol) {
        final double KFPRG = 70.0;
        double FPBRKDS = 1.016; // VE
        final double KFFWTBR = 0.02;
        final double VPSSPLS = 1.016; // pressure drop over the throttle plate

        double fho = pu/1013.0;

        double pirg = fho*KFPRG;

        double pbr = ps * FPBRKDS;
        double psagr = 250; // ?

        double evtmod = tans + (tmot - tans) * KFFWTBR;
        double fwft = (tans+673.425)/731.334;
        double ftbr = 273.0/(evtmod+273)*fwft;

        double fupsrl = kfurl * ftbr;

        double rfagr = Math.max(pbr-pirg, 0)*fupsrl*psagr/ps;

        return (plsol * fupsrl * FPBRKDS * VPSSPLS) - rfagr;
    }
}
