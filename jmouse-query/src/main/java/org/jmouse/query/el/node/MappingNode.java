package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.SourceWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code mapping request { … }} — where one structure's values actually are.
 *
 * <pre>
 * mapping request {
 *   from: requests as r key id
 *   bag:  request_fields on request_id key field_code value text_value
 *
 *   attributes {
 *     key:      request_key  in column
 *     priority: "f-priority" in bag
 *   }
 * }
 *
 * mapping request:export { … }
 * </pre>
 *
 * <h2>⚠️ Two kinds of key in one block, kept apart on purpose</h2>
 *
 * <p>{@code from:}, {@code bag:}, {@code join:} and {@code collection:} are the language's words.
 * Everything inside {@code attributes { }} is a <strong>user's</strong> name. They are in different
 * blocks so that a word the language gains next year cannot shadow an attribute somebody called
 * {@code limit} today.</p>
 *
 * <h2>⚠️ A mapping is NAMED, never vendored</h2>
 *
 * <p>{@code mapping request:export} is a second binding of {@code request}, and the name after the colon
 * affects exactly one thing: which mapping is taken when a view says {@code from: request}. It does not
 * influence the SQL generated — that is the dialect, read from the connection — and it does not influence
 * what is allowed, which is the translator's capabilities.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class MappingNode extends AbstractExpression {

    private static final String INDENT = "  ";

    private final List<AttributeNode>  attributes  = new ArrayList<>();
    private final List<JoinNode>       joins       = new ArrayList<>();
    private final List<CollectionNode> collections = new ArrayList<>();

    private String  structure;
    private String  variant;
    private String  table;
    private String  alias;
    private String  key;
    private String  file;
    private String  delimiter;
    private boolean header;
    private String  rows;
    private BagNode bag;
    private boolean identity;

    /**
     * {@code file: 'requests.csv', header: true, delimiter: ';'} — the rows are in a file.
     *
     * <p>⚠️ A file is not a BACKEND. It is a mapping whose rows come from somewhere else, read by the same
     * translator that reads a list of maps. The moment a file has a translator of its own, the row
     * pipeline has two implementations that agree until one of them is fixed.</p>
     */
    public Optional<String> getFile() {
        return Optional.ofNullable(file);
    }

    public void setFile(String file) {
        this.file = file;
    }

    /** What separates one cell from the next. Absent means the reader's own default. */
    public Optional<String> getDelimiter() {
        return Optional.ofNullable(delimiter);
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /** Whether the first line names the columns rather than carrying data. */
    public boolean hasHeader() {
        return header;
    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    /**
     * {@code from: $rows} — the rows are handed in by whatever runs the query.
     *
     * <p>⚠️ The file names the BINDING, never the data. A mapping able to name a source of its own would
     * be a mapping able to reach something nobody gave it.</p>
     */
    public Optional<String> getRows() {
        return Optional.ofNullable(rows);
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

    /** The structure this binds. */
    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    /** The name after the colon, where this is not the structure's default mapping. */
    public Optional<String> getVariant() {
        return Optional.ofNullable(variant);
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    /** {@code request} or {@code request:export} — how a runtime asks for this one. */
    public String getQualifiedName() {
        return variant == null ? structure : structure + ":" + variant;
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

    public Optional<BagNode> getBag() {
        return Optional.ofNullable(bag);
    }

    public void setBag(BagNode bag) {
        this.bag = bag;
    }

    /**
     * {@code attributes: identity} — every attribute reads the entry of its own name.
     *
     * <p>The ordinary case for rows already keyed the way a query writes them, and shorthand for an
     * {@code attributes { }} block that would say the same thing once per line.</p>
     */
    public boolean isIdentity() {
        return identity;
    }

    public void setIdentity(boolean identity) {
        this.identity = identity;
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

    public List<JoinNode> getJoins() {
        return List.copyOf(joins);
    }

    public void addCollection(CollectionNode collection) {
        collections.add(collection);
    }

    public List<CollectionNode> getCollections() {
        return List.copyOf(collections);
    }

    @Override
    public String toSource() {
        List<String> lines = new ArrayList<>();

        if (table != null) {
            lines.add("%sfrom: %s as %s key %s".formatted(
                    INDENT, SourceWriter.name(table), alias, SourceWriter.name(key)));
        }

        if (rows != null) {
            lines.add("%sfrom: $%s".formatted(INDENT, rows));
        }

        if (file != null) {
            StringBuilder written = new StringBuilder(INDENT).append("file: ")
                    .append(SourceWriter.literal(file));

            if (header) {
                written.append(", header: true");
            }

            if (delimiter != null) {
                written.append(", delimiter: ").append(SourceWriter.literal(delimiter));
            }

            lines.add(written.toString());
        }

        getBag().ifPresent(declared -> lines.add(INDENT + declared.toSource()));

        joins.stream().map(join -> INDENT + join.toSource()).forEach(lines::add);
        collections.stream().map(collection -> INDENT + collection.toSource()).forEach(lines::add);

        if (identity) {
            lines.add(INDENT + "attributes: identity");
        } else if (!attributes.isEmpty()) {
            lines.add(INDENT + "attributes {");
            attributes.stream()
                    .map(attribute -> INDENT + INDENT + attribute.bindingToSource())
                    .forEach(lines::add);
            lines.add(INDENT + "}");
        }

        return "mapping %s {\n%s\n}".formatted(getQualifiedName(), String.join("\n", lines));
    }

    @Override
    public String toString() {
        return "mapping %s (%d attributes)".formatted(getQualifiedName(), attributes.size());
    }
}
