package org.jmouse.cad.spice;

import org.jmouse.cad.spice.SpiceNetlist.ModelType;
import org.jmouse.units.Temperatures;

import java.util.List;
import java.util.Optional;

/**
 * A device a SPICE {@code .model} describes, as jMX knows it ({@link SpiceDevices}): its component type and polarity, its
 * pins in SPICE's order, the component that carries a model naming no component of the library, and what each of its
 * parameters is called on either side.
 *
 * @param element   the letter of the element line that takes a model of it: {@code Q} for NPN and PNP
 * @param polarity  the {@code polarity} parameter its components give it — 1 for NPN and n-channel, -1 for PNP and
 *                  p-channel —, or empty for a device that has none
 * @param carrier   the component a model of it becomes when the model's name names none of the library, by slug
 * @param geometry  what the device line's width and length do to its model, or empty for a device whose line writes none
 */
public record SpiceDevice(String id, ModelType modelType, String element, String componentType, Optional<Double> polarity,
                          List<String> pins,
                          String carrier, List<ParameterName> parameters, Optional<Geometry> geometry) {

    public SpiceDevice {
        pins = List.copyOf(pins);
        parameters = List.copyOf(parameters);
    }

    /**
     * One parameter both sides name.
     *
     * @param spice        its SPICE name, upper case
     * @param jmx          its jMX name
     * @param spiceDefault what SPICE takes when it is not written, as SPICE writes it; empty when jMX cannot write that —
     *                     SPICE's none: no resistance, no breakdown, no limit
     * @param temperature  whether SPICE writes it in °C where jMX writes kelvin ({@code TNOM})
     */
    public record ParameterName(String spice, String jmx, Optional<Double> spiceDefault, boolean temperature) {

        /** A jMX value as SPICE writes it. */
        public double toSpice(double jmxValue) {
            return temperature ? Temperatures.celsius(jmxValue) : jmxValue;
        }

        /** A SPICE value as jMX writes it. */
        public double toJmx(double spiceValue) {
            return temperature ? Temperatures.kelvin(spiceValue) : spiceValue;
        }
    }

    /**
     * What a device line's width and length do to a model: a jMX MOSFET is the whole channel, W = L = 1, so some of its
     * parameters are multiplied by W / L and some by W.
     *
     * @param fallback what each is when the line writes it not, metres
     * @param byRatio  the jMX parameters multiplied by W / L
     * @param byWidth  the jMX parameters multiplied by W: written per metre of width
     */
    public record Geometry(double fallback, List<String> byRatio, List<String> byWidth) {

        public Geometry {
            byRatio = List.copyOf(byRatio);
            byWidth = List.copyOf(byWidth);
        }
    }

    public Optional<ParameterName> bySpiceName(String spice) {
        return parameters.stream().filter(parameter -> parameter.spice().equalsIgnoreCase(spice)).findFirst();
    }

    public Optional<ParameterName> byJmxName(String jmx) {
        return parameters.stream().filter(parameter -> parameter.jmx().equals(jmx)).findFirst();
    }
}
