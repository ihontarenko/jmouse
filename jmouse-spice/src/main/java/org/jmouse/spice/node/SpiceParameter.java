package org.jmouse.spice.node;

import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.spice.SpicePosition;

/**
 * {@code name=value}: a {@code .param}, a model's parameter, a subcircuit's default or an instance's value.
 *
 * @param name as written; SPICE compares names without regard to case
 */
public record SpiceParameter(String name, SpiceValue value, SpicePosition position) implements SourceWritable {

    /** A parameter a program writes. */
    public static SpiceParameter of(String name, SpiceValue value) {
        return new SpiceParameter(name, value, SpicePosition.NONE);
    }

    /** {@code name=value}, as SPICE writes it. */
    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(name).opening("=").write(value);
    }

    /** The parameter as SPICE writes it, on its own. */
    public String written() {
        return TextSourceWriter.render(this, SpiceNode.LAYOUT);
    }
}
