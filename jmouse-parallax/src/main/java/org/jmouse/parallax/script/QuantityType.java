package org.jmouse.parallax.script;

import org.jmouse.el.type.ValueType;

import java.util.Objects;

/**
 * A physical quantity as a type: {@code let I_through : current = …} is a {@code double} that is a current, in its SI
 * unit. The carrier is always {@link Double} — the equations compile to doubles and to Newton's tapes, and a type never
 * reaches them; what the type adds is what the value means, for a reader, for the highlighting, and for the unit a
 * figure is shown in.
 *
 * @param name the name a document writes after the colon
 * @param unit      the SI unit a value of it is in, as a reader writes it ({@code V}, {@code A}, {@code Ω}); empty for
 *                  a ratio
 * @param dimension what it is measured in, as powers of the SI base units — what an expression assigned to it must be
 */
public record QuantityType(String name, String unit, Dimension dimension) implements ValueType {

    public QuantityType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(dimension, "dimension");
    }

    @Override
    public Class<?> carrier() {
        return Double.class;
    }

    @Override
    public String toString() {
        return name + (unit.isEmpty() ? "" : " [" + unit + "]");
    }
}
