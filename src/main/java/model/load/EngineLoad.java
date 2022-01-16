package model.load;

public class EngineLoad {

    // https://mechanics.stackexchange.com/a/17548

    /**
     *
     * @param airflow g/sec
     * @param rpm the engine RPM
     * @param displacement engine displacement in liters
     * @return
     */
    public static double getAbsoluteEngineLoad(double airflow, double rpm, double displacement) {
        double airMass = airflow/((rpm/60d));
        return airMass/(1.184 * displacement)*2;
    }

    /**
     *
     * @param engineLoad %
     * @param rpm the engine RPM
     * @param displacement engine displacement in liters
     * @return
     */
    public static double getAirflow(double engineLoad, double rpm, double displacement) {
        return (engineLoad*(1.184 * displacement)/2)*(rpm/60d);
    }
}
