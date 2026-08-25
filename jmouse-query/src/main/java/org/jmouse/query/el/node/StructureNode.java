package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.SourceWriter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code structure request { … }} — the shape, apart from anything that stores it.
 *
 * <pre>
 * structure request {
 *   key:      string
 *   priority: int, default: 3
 *   tags:     string[]
 * }
 * </pre>
 *
 * <h2>⚠️ Why the shape is not part of the mapping</h2>
 *
 * <p>A structure is <strong>cross-product</strong>; a mapping is not. Kept together — as they were, in a
 * single {@code source} block — a shape could have exactly one binding, and "the same query over a
 * database, a file and a list of maps" was true of the language and of nothing anybody could write.</p>
 *
 * <p>Split, one structure has as many mappings as it needs and a view names the <em>structure</em>. Which
 * mapping runs is the runtime's decision, which is precisely what keeps a view portable.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StructureNode extends AbstractExpression {

    private static final String INDENT = "  ";

    private final List<FieldNode> fields = new ArrayList<>();

    private String name;
    private String baseStructure;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The structure this one extends, written in the header — {@code structure entry : base { … }}.
     *
     * <p>⚠️ In the header rather than as a {@code base:} line, because every other key in this block is
     * a <strong>user's</strong> attribute name. A directive sharing that shape means an attribute called
     * {@code parent} stops being an attribute — and it breaks in somebody else's product a year later,
     * not in the parser.</p>
     */
    public Optional<String> getBaseStructure() {
        return Optional.ofNullable(baseStructure);
    }

    public void setBaseStructure(String baseStructure) {
        this.baseStructure = baseStructure;
    }

    public void addField(FieldNode field) {
        fields.add(field);
    }

    public List<FieldNode> getFields() {
        return List.copyOf(fields);
    }

    /** The fields by name, for a mapping looking up what it is binding. */
    public Map<String, FieldNode> fieldsByName() {
        Map<String, FieldNode> byName = new LinkedHashMap<>();

        for (FieldNode field : fields) {
            byName.put(field.getName(), field);
        }

        return byName;
    }

    @Override
    public String toSource() {
        List<String> lines = new ArrayList<>();

        fields.stream().map(field -> INDENT + field.toSource()).forEach(lines::add);

        return "structure %s%s {\n%s\n}".formatted(
                SourceWriter.name(name),
                baseStructure == null ? "" : " : " + SourceWriter.name(baseStructure),
                String.join("\n", lines));
    }

    @Override
    public String toString() {
        return "structure %s (%d attributes)".formatted(name, fields.size());
    }
}
