package data.contract;

public class Me7LogFileContract {
    public static final String START_TIME_LABEL = "start_time";
    public static final String TIME_COLUMN_LABEL = "TIME";
    public static final String RPM_COLUMN_LABEL = "nmot";
    public static final String STFT_COLUMN_LABEL = "fr_w";
    public static final String LTFT_COLUMN_LABEL = "fra_w";
    public static final String MAF_VOLTAGE_LABEL = "uhfm_w";
    public static final String MAF_GRAMS_PER_SECOND_LABEL = "mshfm_w";
    public static final String THROTTLE_PLATE_ANGLE_LABEL = "wdkba";
    public static final String LAMBDA_CONTROL_ACTIVE_LABEL = "B_lr";
    public static final String REQUESTED_LAMBDA_LABEL = "lamsbg_w";
    public static final String FUEL_INJECTOR_ON_TIME_LABEL = "ti_b1";
    public static final String ENGINE_LOAD_LABEL = "rl_w";
    public static final String WASTEGATE_DUTY_CYCLE_LABEL = "ldtvm";
    public static final String BAROMETRIC_PRESSURE_LABEL = "pus_w";
    public static final String ABSOLUTE_BOOST_PRESSURE_ACTUAL_LABEL = "pvdks_w";
    public static final String SELECTED_GEAR_LABEL = "gangi";
    public static final String WIDE_BAND_O2_LABEL = "lamsoni_w";
    
    public enum Header {
        START_TIME_HEADER(START_TIME_LABEL, "Start Time"),
        TIME_STAMP_COLUMN_HEADER(TIME_COLUMN_LABEL, "Timestamp"),
        RPM_COLUMN_HEADER(RPM_COLUMN_LABEL, "RPM"),
        STFT_COLUMN_HEADER(STFT_COLUMN_LABEL, "Short Term Fuel Trim"),
        LTFT_COLUMN_HEADER(LTFT_COLUMN_LABEL, "Long Term Fuel Trim"),
        MAF_VOLTAGE_HEADER(MAF_VOLTAGE_LABEL, "MAF Voltage"),
        MAF_GRAMS_PER_SECOND_HEADER(MAF_GRAMS_PER_SECOND_LABEL, "MAF g/sec"),
        THROTTLE_PLATE_ANGLE_HEADER(THROTTLE_PLATE_ANGLE_LABEL, "Throttle Plate Angle"),
        LAMBDA_CONTROL_ACTIVE_HEADER(LAMBDA_CONTROL_ACTIVE_LABEL, "Lambda Control"),
        REQUESTED_LAMBDA_HEADER(REQUESTED_LAMBDA_LABEL, "Requested Lambda"),
        FUEL_INJECTOR_ON_TIME_HEADER(FUEL_INJECTOR_ON_TIME_LABEL, "Fuel Injector On-Time"),
        ENGINE_LOAD_HEADER(ENGINE_LOAD_LABEL, "Engine Load"),
        WASTEGATE_DUTY_CYCLE_HEADER(WASTEGATE_DUTY_CYCLE_LABEL, "Wastegate Duty Cycle"),
        BAROMETRIC_PRESSURE_HEADER(BAROMETRIC_PRESSURE_LABEL, "Barometric Pressure"),
        ABSOLUTE_BOOST_PRESSURE_ACTUAL_HEADER(ABSOLUTE_BOOST_PRESSURE_ACTUAL_LABEL, "Absolute Pressure"),
        SELECTED_GEAR_HEADER(SELECTED_GEAR_LABEL, "Selected Gear"),
        WIDE_BAND_O2_HEADER(WIDE_BAND_O2_LABEL, "Wide Band O2");

        private String header;
        private final String title;

        Header(String header, String title) {
            this.header = header;
            this.title = title;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getTitle() {
            return title;
        }
    }


}
