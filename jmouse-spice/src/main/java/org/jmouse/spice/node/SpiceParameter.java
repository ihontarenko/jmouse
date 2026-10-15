package org.jmouse.spice.node;

import org.jmouse.spice.SpicePosition;

/**
 * {@code name=value}: a {@code .param}, a model's parameter, a subcircuit's default or an instance's value.
 *
 * @param name as written; SPICE compares names without regard to case
 */
public record SpiceParameter(String name, SpiceValue value, SpicePosition position) {

    /** A parameter a program writes. */
    public static SpiceParameter of(String name, SpiceValue value) {
        return new SpiceParameter(name, value, SpicePosition.NONE);
    }

    /** {@code name=value}, as SPICE writes it. */
    public String written() {
        return name + "=" + value.written();
    }
}
