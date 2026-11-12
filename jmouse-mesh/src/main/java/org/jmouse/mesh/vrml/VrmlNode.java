package org.jmouse.mesh.vrml;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A VRML97 node as written — {@code Material { diffuseColor 0.8 0.8 0.8 }} —: its type, the name a {@code DEF} gave it,
 * and its fields by name. A {@code USE} is the very node its {@code DEF} made.
 *
 * @param name the {@code DEF} name; null when it has none
 */
public record VrmlNode(String type, String name, Map<String, VrmlValue> fields) {

    public VrmlNode {
        fields = Map.copyOf(fields);
    }

    /** A field's numbers; empty when it is not written or holds no numbers. */
    public Optional<double[]> numbers(String field) {
        return Optional.ofNullable(fields.get(field)).filter(VrmlValue.Numbers.class::isInstance)
                .map(value -> ((VrmlValue.Numbers) value).values());
    }

    /** A field's one node: {@code appearance Appearance { … }}; empty when it is not written or is NULL. */
    public Optional<VrmlNode> node(String field) {
        VrmlValue value = fields.get(field);
        if (value instanceof VrmlValue.Node node) {
            return Optional.of(node.node());
        }
        if (value instanceof VrmlValue.Nodes nodes && nodes.nodes().size() == 1) {
            return Optional.of(nodes.nodes().getFirst());
        }
        return Optional.empty();
    }

    /** A field's nodes: {@code children [ … ]}, or the one node written without brackets. */
    public List<VrmlNode> nodes(String field) {
        VrmlValue value = fields.get(field);
        if (value instanceof VrmlValue.Nodes nodes) {
            return nodes.nodes();
        }
        if (value instanceof VrmlValue.Node node) {
            return List.of(node.node());
        }
        return List.of();
    }

    /** A field's word — {@code TRUE}, {@code FALSE}; empty when it is not one. */
    public Optional<String> word(String field) {
        VrmlValue value = fields.get(field);
        return value instanceof VrmlValue.Words words && !words.words().isEmpty() ? Optional.of(words.words().getFirst())
                : Optional.empty();
    }
}
