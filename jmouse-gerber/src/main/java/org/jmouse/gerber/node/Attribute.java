package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.util.List;

/**
 * {@code %TF.FileFunction,Copper,L1,Top*%}: an attribute — its scope, its name, its values. A delete names the
 * attribute it ends, or none for all of them ({@code %TD*%}).
 */
public final class Attribute extends GerberCommand {

    private final AttributeScope scope;
    private final String         name;
    private final List<String>   values;

    public Attribute(AttributeScope scope, String name, List<String> values, GerberPosition position) {
        super(position);
        this.scope = scope;
        this.name = name;
        this.values = values.stream().map(Attribute::field).toList();
    }

    /** An attribute a program writes. */
    public static Attribute of(AttributeScope scope, String name, String... values) {
        return new Attribute(scope, name, List.of(values), GerberPosition.NONE);
    }

    /** The delete of every aperture and object attribute in force. */
    public static Attribute deleteAll() {
        return new Attribute(AttributeScope.DELETE, "", List.of(), GerberPosition.NONE);
    }

    public AttributeScope scope() {
        return scope;
    }

    /** {@code .FileFunction}: a standard name starts with its point; empty for a delete of all. */
    public String name() {
        return name;
    }

    public List<String> values() {
        return values;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        extended(writer, body -> {
            body.word(scope.command()).word(name);
            values.forEach(value -> body.join(",").word(value));
        });
    }

    /** A value a field may hold: no comma, asterisk or percent sign, which end fields and commands. */
    private static String field(String text) {
        return text.replaceAll("[,*%\\n\\r]", " ").strip();
    }
}
