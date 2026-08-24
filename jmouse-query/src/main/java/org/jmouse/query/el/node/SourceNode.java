package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.SourceWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code source inventory { … }} — where a product's rows are, and what may be asked of them.
 *
 * <pre>
 * source inventory {
 *   from      form_entries as e key id
 *   bag       field_entries on form_entry_id key field_id value text_value
 *   attribute entry[name]     from "f-component-name" text     in bag
 *   attribute entry[quantity] from "f-quantity"       unknown  in bag
 *   attribute created         from created_at         temporal in column
 * }
 * </pre>
 *
 * <h2>⚠️ Declared in this language rather than in a companion file</h2>
 *
 * <p>A <strong>view is cross-product; a mapping is not</strong>. {@code view … on inventory} should run
 * in any product that declares an {@code inventory} source — so the mapping has to live apart from the
 * view, or the view stops being portable, which was the point of having it.</p>
 *
 * <p>Apart, yes. A separate <em>format</em>, no: one grammar means one parser, one highlighter and one
 * un-parse. It is what {@code .jmp} does, which carries no sidecar for its scopes and declares
 * {@code scopes { }} in its own syntax.</p>
 *
 * <h2>⚠️ What a source may never declare</h2>
 *
 * <p>A tenant or workspace scope. That is injected by whatever runs the query. A file able to name its
 * own scope is a file that can read somebody else's data — and unlike a mapping written in Java, this
 * one is edited by people who never read the code.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SourceNode extends AbstractExpression {

    private static final String INDENT = "  ";

    private final List<AttributeNode>  attributes  = new ArrayList<>();
    private final List<JoinNode>       joins       = new ArrayList<>();
    private final List<CollectionNode> collections = new ArrayList<>();

    private String  name;
    private String  table;
    private String  alias;
    private String  key;
    private BagNode bag;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /** The bag this source's loose values live in, where it has one. */
    public Optional<BagNode> getBag() {
        return Optional.ofNullable(bag);
    }

    public void setBag(BagNode bag) {
        this.bag = bag;
    }

    public void addAttribute(AttributeNode attribute) {
        attributes.add(attribute);
    }

    public List<AttributeNode> getAttributes() {
        return List.copyOf(attributes);
    }

    public void addJoin(JoinNode join) {
        joins.add(join);
    }

    /** The tables one hop away, in declaration order. */
    public List<JoinNode> getJoins() {
        return List.copyOf(joins);
    }

    public void addCollection(CollectionNode collection) {
        collections.add(collection);
    }

    /** The many-rows-per-row tables, in declaration order. */
    public List<CollectionNode> getCollections() {
        return List.copyOf(collections);
    }

    @Override
    public String toSource() {
        List<String> lines = new ArrayList<>();

        lines.add("%sfrom %s as %s key %s".formatted(
                INDENT, SourceWriter.name(table), alias, SourceWriter.name(key)));

        getBag().ifPresent(declared -> lines.add(INDENT + declared.toSource()));

        joins.stream().map(join -> INDENT + join.toSource()).forEach(lines::add);
        collections.stream().map(collection -> INDENT + collection.toSource()).forEach(lines::add);

        attributes.stream().map(attribute -> INDENT + attribute.toSource()).forEach(lines::add);

        return "source %s {\n%s\n}".formatted(
                SourceWriter.name(name), lines.stream().collect(Collectors.joining("\n")));
    }

    @Override
    public String toString() {
        return "source %s (%d attributes)".formatted(name, attributes.size());
    }
}
