package org.jmouse.parallax.spice;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Which component a SPICE {@code .model} becomes when its name names no component of the library.
 *
 * <p>A {@code .model} is a device with numbers; a jMX part is a component with parameters. A diode model that writes
 * only {@code IS} is the engine's own diode exactly. One that writes more needs a component whose equations take those
 * parameters — the SPICE junction, its series resistance, its charge — and the carrier is that component: the part is
 * written as it, with the model's parameters set on it. Which components carry which device is the product's catalogue,
 * so it is configuration here; a device without a carrier is noted and left out.</p>
 *
 * @param references each device → the reference of the component that carries it
 */
public record SpiceModelCarriers(Map<SpiceDevice, String> references) {

    public SpiceModelCarriers {
        references = references.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(references));
    }

    /**
     * The shipped catalogue's general devices, whose equations take every parameter of their model: the diode, the
     * NPN and PNP transistors (the Gummel–Poon model), the n- and p-channel MOSFETs and JFETs (level 1).
     */
    public static SpiceModelCarriers standard() {
        return new SpiceModelCarriers(Map.of(SpiceDevice.DIODE, "diode", SpiceDevice.NPN, "npn-transistor",
                SpiceDevice.PNP, "pnp-transistor", SpiceDevice.NMOS, "n-mosfet", SpiceDevice.PMOS, "p-mosfet",
                SpiceDevice.NJF, "n-jfet", SpiceDevice.PJF, "p-jfet"));
    }

    public Optional<String> carrierOf(SpiceDevice device) {
        return Optional.ofNullable(references.get(device));
    }
}
