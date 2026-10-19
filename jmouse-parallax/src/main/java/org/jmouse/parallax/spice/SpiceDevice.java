package org.jmouse.parallax.spice;

import org.jmouse.parallax.component.PhysicalConstants;

import org.jmouse.parallax.spice.SpiceNetlist.ModelType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The semiconductor devices both languages have, and the one table both directions read: which SPICE model parameter
 * is which jMX component parameter, and what SPICE takes when a model does not write it.
 *
 * <h2>Where the jMX names come from</h2>
 *
 * <p>From the component documents that model these devices: a diode's {@code I_saturation}, {@code R_series},
 * {@code C_junction_zero_bias}; a transistor's {@code beta_forward}, {@code V_early}. A component that writes these
 * names is read as the device by its {@code type} ({@code diode}, {@code bjt}, {@code mosfet}) and its pins
 * ({@link #pins()}), and its polarity by the symbol it is drawn with — an {@code npn}/{@code pnp} or
 * {@code n-channel}/{@code p-channel} symbol. jMX carries no SPICE word for any of it.</p>
 *
 * <h2>Defaults</h2>
 *
 * <p>{@link ParameterName#spiceDefault()} is what SPICE takes when the model omits the parameter, and it is written into
 * the jMX part so that the part means what the SPICE model means. {@code null} marks a default jMX cannot write: a
 * series resistance of zero, which a component's equations divide by, and an infinite breakdown or Early voltage. Such
 * a parameter keeps the component's own value, and the conversion says so in a note.</p>
 */
public enum SpiceDevice {

    DIODE(ModelType.D, "diode", List.of("anode", "cathode"), List.of(
            new ParameterName("IS", "I_saturation", 1e-14, false),
            new ParameterName("N", "emission_coefficient", 1.0, false),
            new ParameterName("RS", "R_series", null, false),
            new ParameterName("ISR", "I_recombination", 0.0, false),
            new ParameterName("NR", "recombination_emission_coefficient", 2.0, false),
            new ParameterName("IKF", "I_knee", null, false),
            new ParameterName("CJO", "C_junction_zero_bias", 0.0, false),
            new ParameterName("VJ", "V_junction", 1.0, false),
            new ParameterName("M", "grading_coefficient", 0.5, false),
            new ParameterName("FC", "forward_bias_coefficient", 0.5, false),
            new ParameterName("TT", "transit_time", 0.0, false),
            new ParameterName("BV", "V_breakdown", null, false),
            new ParameterName("IBV", "I_breakdown", null, false),
            new ParameterName("XTI", "saturation_temperature_exponent", 3.0, false),
            new ParameterName("EG", "E_gap", 1.11, false),
            new ParameterName("TNOM", "T_nominal", 27.0, true))),

    NPN(ModelType.NPN, "bjt", List.of("collector", "base", "emitter"), Tables.BIPOLAR),

    PNP(ModelType.PNP, "bjt", List.of("collector", "base", "emitter"), Tables.BIPOLAR),

    NMOS(ModelType.NMOS, "mosfet", List.of("drain", "gate", "source"), Tables.MOSFET),

    PMOS(ModelType.PMOS, "mosfet", List.of("drain", "gate", "source"), Tables.MOSFET),

    NJF(ModelType.NJF, "jfet", List.of("drain", "gate", "source"), Tables.JFET),

    PJF(ModelType.PJF, "jfet", List.of("drain", "gate", "source"), Tables.JFET);

    /**
     * One parameter both sides name.
     *
     * @param spice        its SPICE name, upper case
     * @param jmx          its jMX name
     * @param spiceDefault what SPICE takes when it is not written; {@code null} when jMX cannot write that
     * @param temperature  whether SPICE writes it in °C where jMX writes kelvin ({@code TNOM})
     */
    public record ParameterName(String spice, String jmx, Double spiceDefault, boolean temperature) {

        /** A jMX value as SPICE writes it. */
        public double toSpice(double jmxValue) {
            return temperature ? Math.rint((jmxValue - PhysicalConstants.ZERO_CELSIUS) * 1e9) / 1e9 : jmxValue;
        }

        /** A SPICE value as jMX writes it. */
        public double toJmx(double spiceValue) {
            return temperature ? Math.rint((spiceValue + PhysicalConstants.ZERO_CELSIUS) * 1e9) / 1e9 : spiceValue;
        }
    }

    private final ModelType           modelType;
    private final String              componentType;
    private final List<String>        pins;
    private final List<ParameterName> parameters;

    SpiceDevice(ModelType modelType, String componentType, List<String> pins, List<ParameterName> parameters) {
        this.modelType = modelType;
        this.componentType = componentType;
        this.pins = pins;
        this.parameters = parameters;
    }

    /** The {@code .model} type: {@code D}, {@code NPN} … */
    public ModelType modelType() {
        return modelType;
    }

    /** The jMX component {@code type} a component modelling it carries. */
    public String componentType() {
        return componentType;
    }

    /** Its pins in SPICE's order, by the names jMX components give them. */
    public List<String> pins() {
        return pins;
    }

    public List<ParameterName> parameters() {
        return parameters;
    }

    public Optional<ParameterName> bySpiceName(String spice) {
        return parameters.stream().filter(parameter -> parameter.spice().equalsIgnoreCase(spice)).findFirst();
    }

    public Optional<ParameterName> byJmxName(String jmx) {
        return parameters.stream().filter(parameter -> parameter.jmx().equals(jmx)).findFirst();
    }

    public static SpiceDevice of(ModelType type) {
        return Arrays.stream(values()).filter(device -> device.modelType == type).findFirst().orElseThrow();
    }

    /**
     * The device a component is, from its {@code type} and the symbols it is drawn with; empty for a component that is
     * none of these, or whose polarity its symbols do not say.
     */
    public static Optional<SpiceDevice> ofComponent(String type, List<String> symbols) {
        String drawn = String.join(" ", symbols).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "diode" -> Optional.of(DIODE);
            case "bjt" -> drawn.contains("pnp") ? Optional.of(PNP) : drawn.contains("npn") ? Optional.of(NPN) : Optional.empty();
            case "mosfet" -> drawn.contains("p-channel") ? Optional.of(PMOS)
                    : drawn.contains("n-channel") ? Optional.of(NMOS) : Optional.empty();
            case "jfet" -> drawn.contains("p-channel") ? Optional.of(PJF)
                    : drawn.contains("n-channel") ? Optional.of(NJF) : Optional.empty();
            default -> Optional.empty();
        };
    }

    private static final class Tables {

        static final List<ParameterName> BIPOLAR = List.of(
                new ParameterName("IS", "I_saturation", 1e-16, false),
                new ParameterName("BF", "beta_forward", 100.0, false),
                new ParameterName("NF", "emission_coefficient", 1.0, false),
                new ParameterName("VAF", "V_early", null, false),
                new ParameterName("IKF", "I_knee", null, false),
                new ParameterName("ISE", "I_leakage_base_emitter", 0.0, false),
                new ParameterName("NE", "leakage_emission_base_emitter", 1.5, false),
                new ParameterName("BR", "beta_reverse", 1.0, false),
                new ParameterName("NR", "emission_coefficient_reverse", 1.0, false),
                new ParameterName("VAR", "V_early_reverse", null, false),
                new ParameterName("IKR", "I_knee_reverse", null, false),
                new ParameterName("ISC", "I_leakage_base_collector", 0.0, false),
                new ParameterName("NC", "leakage_emission_base_collector", 2.0, false),
                new ParameterName("RB", "R_base", null, false),
                new ParameterName("RBM", "R_base_minimum", null, false),
                new ParameterName("RE", "R_emitter", null, false),
                new ParameterName("RC", "R_collector", null, false),
                new ParameterName("CJE", "C_base_emitter", 0.0, false),
                new ParameterName("VJE", "V_junction_base_emitter", 0.75, false),
                new ParameterName("MJE", "grading_base_emitter", 0.33, false),
                new ParameterName("CJC", "C_base_collector", 0.0, false),
                new ParameterName("VJC", "V_junction_base_collector", 0.75, false),
                new ParameterName("MJC", "grading_base_collector", 0.33, false),
                new ParameterName("XCJC", "internal_base_collector_fraction", 1.0, false),
                new ParameterName("FC", "forward_bias_coefficient", 0.5, false),
                new ParameterName("TF", "transit_time_forward", 0.0, false),
                new ParameterName("XTF", "transit_time_bias_dependence", 0.0, false),
                new ParameterName("VTF", "transit_time_bias_voltage", null, false),
                new ParameterName("ITF", "transit_time_current", 0.0, false),
                new ParameterName("TR", "transit_time_reverse", 0.0, false),
                new ParameterName("XTB", "beta_temperature_exponent", 0.0, false),
                new ParameterName("XTI", "saturation_temperature_exponent", 3.0, false),
                new ParameterName("EG", "E_gap", 1.11, false),
                new ParameterName("TNOM", "T_nominal", 27.0, true));

        // Level 1 with W = L = 1 on the device line, so KP is the whole channel and CGSO, CGDO are farads.
        static final List<ParameterName> MOSFET = List.of(
                new ParameterName("VTO", "V_threshold", 0.0, false),
                new ParameterName("KP", "transconductance", 2e-5, false),
                new ParameterName("LAMBDA", "channel_length_modulation", 0.0, false),
                new ParameterName("RD", "R_drain", null, false),
                new ParameterName("RS", "R_source", null, false),
                new ParameterName("IS", "I_saturation", 1e-14, false),
                new ParameterName("N", "emission_coefficient", 1.0, false),
                new ParameterName("CBD", "C_bulk_drain", 0.0, false),
                new ParameterName("PB", "V_junction", 0.8, false),
                new ParameterName("MJ", "grading_coefficient", 0.5, false),
                new ParameterName("FC", "forward_bias_coefficient", 0.5, false),
                new ParameterName("CGSO", "C_gate_source", 0.0, false),
                new ParameterName("CGDO", "C_gate_drain", 0.0, false),
                new ParameterName("TNOM", "T_nominal", 27.0, true));

        // Level 1: VTO keeps its sign for both channels, IDSS = BETA · VTO².
        static final List<ParameterName> JFET = List.of(
                new ParameterName("VTO", "V_threshold", -2.0, false),
                new ParameterName("BETA", "transconductance", 1e-4, false),
                new ParameterName("LAMBDA", "channel_length_modulation", 0.0, false),
                new ParameterName("RD", "R_drain", null, false),
                new ParameterName("RS", "R_source", null, false),
                new ParameterName("IS", "I_saturation", 1e-14, false),
                new ParameterName("N", "emission_coefficient", 1.0, false),
                new ParameterName("CGS", "C_gate_source", 0.0, false),
                new ParameterName("CGD", "C_gate_drain", 0.0, false),
                new ParameterName("PB", "V_junction", 1.0, false),
                new ParameterName("FC", "forward_bias_coefficient", 0.5, false),
                new ParameterName("TNOM", "T_nominal", 27.0, true));

        private Tables() {
        }
    }
}
