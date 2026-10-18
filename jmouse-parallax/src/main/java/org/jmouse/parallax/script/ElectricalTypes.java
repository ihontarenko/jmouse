package org.jmouse.parallax.script;

import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.type.ValueType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The types jMX adds to the core's for the circuits it describes: the electrical quantities, each a {@code double} in
 * its SI unit. Registered like any extension — the language's own types are the core's ({@code int}, {@code double}…)
 * plus these; a document only names them.
 */
public final class ElectricalTypes implements Extension {

    private static final List<ValueType> QUANTITIES = List.of(
            new QuantityType("voltage", "V", Dimension.VOLTAGE),
            new QuantityType("current", "A", Dimension.CURRENT),
            new QuantityType("charge", "C", Dimension.CHARGE),
            new QuantityType("power", "W", Dimension.POWER),
            new QuantityType("energy", "J", Dimension.ENERGY),
            new QuantityType("resistance", "Ω", Dimension.RESISTANCE),
            new QuantityType("conductance", "S", Dimension.CONDUCTANCE),
            new QuantityType("capacitance", "F", Dimension.CAPACITANCE),
            new QuantityType("inductance", "H", Dimension.INDUCTANCE),
            new QuantityType("flux", "Wb", Dimension.FLUX),
            new QuantityType("frequency", "Hz", Dimension.FREQUENCY),
            new QuantityType("time", "s", Dimension.TIME),
            new QuantityType("temperature", "K", Dimension.TEMPERATURE),
            new QuantityType("length", "m", Dimension.LENGTH),
            new QuantityType("area", "m²", Dimension.LENGTH.power(2)),
            new QuantityType("ratio", "", Dimension.NONE)
    );

    /** Every type a jMX document may name: the core's, then the electrical quantities, by name. */
    private static final Map<String, ValueType> KNOWN = known();

    @Override
    public List<ValueType> getTypes() {
        return QUANTITIES;
    }

    /** The type a document names, if jMX knows it. */
    public static Optional<ValueType> find(String name) {
        return Optional.ofNullable(KNOWN.get(name));
    }

    /** The names of every type a jMX document may write, for a refusal to list. */
    public static List<String> names() {
        return KNOWN.keySet().stream().sorted().toList();
    }

    private static Map<String, ValueType> known() {
        Map<String, ValueType> known = new LinkedHashMap<>();
        new CoreExtension().getTypes().forEach(type -> known.put(type.name(), type));
        QUANTITIES.forEach(type -> known.put(type.name(), type));
        return Map.copyOf(known);
    }
}
