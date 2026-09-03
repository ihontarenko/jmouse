package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.SourceWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /** The file this reads, where its rows are in one. */
    public Optional<String> getFile() {
        return Optional.ofNullable(file);
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Optional<String> getDelimiter() {
        return Optional.ofNullable(delimiter);
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public boolean hasHeader() {
        return header;
    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    /** The binding whose rows this reads, where the runtime hands them in. */
    public Optional<String> getRows() {
        return Optional.ofNullable(rows);
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

    /** The shape this binds, where it came from a {@code mapping} rather than an older {@code source}. */
    public String getStructure() {
        return structure == null ? name : structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    /** The name after the colon — {@code request:export} — where this is not the default mapping. */
    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

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

    /**
     * One shape bound to one place — what everything downstream of the parser consumes.
     *
     * <p>⚠️ Merging here rather than teaching the compiler about two declarations is the whole reason the
     * split cost nothing below the parser: a {@code structure} and a {@code mapping} meet in this object
     * and the loader, the checker and every translator carry on reading exactly what they always read.</p>
     *
     * @param structure the shape — where each attribute's type and default come from
     * @param mapping   the binding — where each attribute's value actually is
     * @return the merged source, named for the mapping
     */
    public static SourceNode merge(StructureNode structure, MappingNode mapping) {
        SourceNode source = new SourceNode();

        source.setName(mapping.getQualifiedName());
        source.setStructure(structure.getName());
        source.setTable(mapping.getTable());
        source.setAlias(mapping.getAlias());
        source.setKey(mapping.getKey());
        source.setHeader(mapping.hasHeader());
        mapping.getFile().ifPresent(source::setFile);
        mapping.getDelimiter().ifPresent(source::setDelimiter);
        mapping.getRows().ifPresent(source::setRows);
        mapping.getBag().ifPresent(source::setBag);

        mapping.getJoins().forEach(source::addJoin);
        mapping.getCollections().forEach(source::addCollection);

        Map<String, FieldNode> fields = structure.fieldsByName();

        for (AttributeNode binding : bindings(structure, mapping)) {
            FieldNode field = fields.get(binding.getName());

            if (field == null) {
                throw new QueryParseException(
                        ("mapping '%s' binds '%s', and structure '%s' does not declare it; "
                         + "it declares %s").formatted(
                                mapping.getQualifiedName(), binding.getName(), structure.getName(),
                                String.join(", ", fields.keySet())));
            }

            // ⚠️ A COPY. The bindings belong to the mapping node, which is also what writes the document
            // back out — renaming them in place qualified every attribute in the file it came from, and
            // the only symptom was a rewritten mapping that no longer matched its own structure.
            AttributeNode merged = new AttributeNode();

            merged.setName(qualify(structure.getName(), binding.getName()));

            // ⚠️ `identity` means "the entry of its own name", and the name a query writes is the QUALIFIED
            // one. Leaving the bare field name here made every identity mapping read a key no row has —
            // and the symptom was an empty result rather than an error.
            merged.setSource(mapping.isIdentity() ? merged.getName() : binding.getSource());
            merged.setAccess(binding.getAccess());
            merged.setType(field.getCanonicalType());

            // ⚠️ From the STRUCTURE, like the type and for the same reason: what an attribute is called
            // is a fact about the shape. Read off the binding instead, two mappings of one structure
            // could label the same attribute differently — a difference visible from neither file.
            merged.setLabel(field.getLabel());

            source.addAttribute(merged);
        }

        return source;
    }

    /**
     * What a query writes for one of a structure's attributes — {@code request.status}.
     *
     * <h2>⚠️ The prefix is the structure's name, and it is written once</h2>
     *
     * <p>A structure lists {@code status:} and a query says {@code request.status}. Repeating the prefix
     * on every line of the declaration is what the older spelling did, and it is one more thing that can
     * be typed differently on one line out of twenty — in a file where a wrong prefix means an attribute
     * nothing can find.</p>
     *
     * <p>⚠️ A name that already carries a dot or a bracket is left exactly as it is. That is what a
     * document in the older spelling declares, and re-prefixing it would rename every attribute in every
     * stored query.</p>
     */
    private static String qualify(String structure, String field) {
        boolean alreadyPathed = field.indexOf('.') >= 0 || field.indexOf('[') >= 0;

        return alreadyPathed ? field : structure + "." + field;
    }

    /**
     * ⚠️ {@code attributes: identity} is expanded HERE rather than left as a flag, so nothing downstream
     * has to know the shorthand exists. Every attribute reads the entry of its own name, in a column.
     */
    private static List<AttributeNode> bindings(StructureNode structure, MappingNode mapping) {
        if (!mapping.isIdentity()) {
            return mapping.getAttributes();
        }

        List<AttributeNode> expanded = new ArrayList<>();

        for (FieldNode field : structure.getFields()) {
            AttributeNode binding = new AttributeNode();

            binding.setName(field.getName());
            binding.setSource(field.getName());
            binding.setAccess("column");

            expanded.add(binding);
        }

        return expanded;
    }

    /**
     * The shape half of this source, as the standard writes it.
     *
     * <p>⚠️ A source read from the older {@code source { }} spelling renders as a {@code structure} and a
     * {@code mapping} like any other, which is what makes rewriting a stored document the whole
     * migration: the reader stays tolerant, the writer only ever emits the current form.</p>
     */
    public StructureNode toStructure() {
        StructureNode declared = new StructureNode();
        String        prefix   = (structure == null ? name : structure) + ".";

        declared.setName(structure == null ? name : structure);

        for (AttributeNode attribute : attributes) {
            FieldNode field = new FieldNode();

            // ⚠️ The prefix comes off, because the structure's name puts it back. A document read in the
            // older spelling therefore comes out declaring `status:` rather than `request.status:`, which
            // is the standard's form and not a rename: the attribute a query writes is unchanged.
            field.setName(attribute.getName().startsWith(prefix)
                    ? attribute.getName().substring(prefix.length())
                    : attribute.getName());
            field.setType(attribute.getType());

            // ⚠️ The label goes to the STRUCTURE half and never to the mapping, which is what makes the
            // older `source { }` spelling — where shape and binding share a line — round-trip into the
            // current one without the label landing in both or in neither.
            field.setLabel(attribute.getLabel());

            declared.addField(field);
        }

        return declared;
    }

    /** The binding half of this source, as the standard writes it. */
    public MappingNode toMapping() {
        MappingNode declared = new MappingNode();

        declared.setStructure(structure == null ? name : structure);
        declared.setVariant(variant);
        declared.setTable(table);
        declared.setAlias(alias);
        declared.setKey(key);
        declared.setHeader(header);
        getFile().ifPresent(declared::setFile);
        getDelimiter().ifPresent(declared::setDelimiter);
        getRows().ifPresent(declared::setRows);
        getBag().ifPresent(declared::setBag);

        joins.forEach(declared::addJoin);
        collections.forEach(declared::addCollection);

        String prefix = (structure == null ? name : structure) + ".";

        for (AttributeNode attribute : attributes) {
            AttributeNode binding = new AttributeNode();

            binding.setName(attribute.getName().startsWith(prefix)
                    ? attribute.getName().substring(prefix.length())
                    : attribute.getName());
            binding.setSource(attribute.getSource());
            binding.setAccess(attribute.getAccess());

            declared.addAttribute(binding);
        }

        return declared;
    }

    @Override
    public String toSource() {
        return "%s\n\n%s".formatted(toStructure().toSource(), toMapping().toSource());
    }

    @Override
    public String toString() {
        return "source %s (%d attributes)".formatted(name, attributes.size());
    }
}
