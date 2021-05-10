package org.grobid.core.utilities;

/**
 * Utilities for managing SI and non-SI units.
 *
 * @author Patrice Lopez
 */
public class UnitUtilities {

    // measurement systems
    public enum System_Type {
        UNKNOWN("unknown"),
        SI_BASE("SI base"),
        SI_DERIVED("SI derived"),
        IMPERIAL("imperial"),
        US("us"),
        CGS("centimetre–gram–second"),
        NON_SI("non SI");

        private String name;

        private System_Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // measurement types
    public enum Unit_Type {
        UNKNOWN("unknown"),
        LENGTH("length"),
        TIME("time"),
        TEMPERATURE("temperature"),
        MASS("mass"),
        LUMINOUS_INTENSITY("luminous intensity"),
        AMOUNT_OF_SUBSTANCE("amount of substance"),
        ELECTRIC_CURRENT("electric current"),
        ANGLE("angle"),
        SOLID_ANGLE("solid angle"),
        FREQUENCY("frequency"),
        FORCE("force"),
        PRESSURE("pressure"),
        ENERGY("energy"),
        POWER("power"),
        ELECTRIC_CHARGE("electric charge"),
        VOLTAGE("voltage"),
        ELECTRIC_CAPACITANCE("electric capacitance"),
        ELECTRIC_RESISTANCE("electric resistance"),
        ELECTRIC_CONDUCTANCE("electric conductance"),
        ELECTRIC_FIELD("electric field"),
        MAGNETIC_FLUX("magnetic flux"),
        MAGNETIC_INDUCTION("magnetic induction"),
        MAGNETIC_FIELD_STRENGTH("magnetic field strength"),
        INDUCTANCE("inductance"),
        LUMINOUS_FLUX("luminous flux"),
        ILLUMINANCE("illuminance"),
        LUMINANCE("luminance"),
        RADIOACTIVITY("radioactivity"),
        ABSORBED_DOSE("absorbed dose"),
        EQUIVALENT_DOSE("equivalent dose"),
        ATTENUATION("attenuation"),
        TORQUE("torque"),
        DYNAMIC_VISCOSITY("dynamic viscosity"),
        KINEMATIC_VISCOSITY("kinematic viscosity"),
        ACOUSTIC_PRESSURE("acoustic pressure"),
        MASS_FLOW_RATE("mass flow-rate"),
        VOLUME_FLOW_RATE("volume flow-rate"),
        AIR_FLOW_RATE("air flow-rate"),
        SPECTRAL_RESPONSITIVY("spectral responsivity"),
        SPECTRAL_TRANSMITTANCE("regular spectral transmittance"),
        SPECTRAL_REFLECTANCE("diffuse spectral reflectance"),
        SPECTRAL_FLUX_DENSITY("spectral flux density"),
        REFLECTANCE("reflectance"),
        DETECTOR_PASSBAND("detector passband"),
        THERMAL_CONDUCTIVITY("thermal conductivity"),
        THERMAL_DIFFUSIVITY("thermal diffusivity"),
        HEAT_CAPACITY("specific heat capacity"),
        EMISSION_RATE("emission rate"),
        CATALYTIC_ACTIVITY("catalytic activity"),
        RADIANCE("radiance"),
        IRRADIANCE("irradiance"),
        EMISSIVITY("emissivity"),
        HUMIDITY("humidity"),
        VOLUME("volume"),
        VELOCITY("velocity"),
        AREA("area"),
        CONCENTRATION("concentration"),
        DENSITY("density"),
        ACIDITY("acidity"),
        FRACTION("fraction"),
        VO2_MAX("VO2 max"),
        COUNT("count"),
        ACCELERATION("acceleration"),
        DEGREE("angle"),
        DIFFUSION_FLUX("diffusion flux"),
        MAGNETIC_MOMENT("magnetic moment"),
        ATOM_MASS_UNIT("atom mass unit"),
        PACE("pace");

        private String name;

        private Unit_Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    ;

    // measurement type (atomic value, interval of conjuctive/disjunctive list of values/intervals)
    public enum Measurement_Type {
        VALUE("value"),
        INTERVAL_MIN_MAX("interval min max"),
        INTERVAL_BASE_RANGE("interval base range"),
        CONJUNCTION("listc"),
        DISJUNCTION("listd");

        private String name;

        private Measurement_Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static volatile UnitUtilities instance;

    public static UnitUtilities getInstance() {
        if (instance == null) {
            //double check idiom
            // synchronized (instanceController) {
            if (instance == null)
                getNewInstance();
            // }
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new UnitUtilities();
    }

    private UnitUtilities() {
    }


}