package org.jmouse.query.sql;

import org.jmouse.query.el.node.AttributeNode;
import org.jmouse.query.el.node.BagNode;
import org.jmouse.query.el.node.QueryDocumentNode;
import org.jmouse.query.el.node.SourceNode;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.schema.QueryType;
import org.jmouse.query.sql.mapping.AttributeMappings;
import org.jmouse.query.sql.mapping.BagMapping;
import org.jmouse.query.sql.mapping.BagTable;
import org.jmouse.query.sql.mapping.CollectionMapping;
import org.jmouse.query.sql.mapping.CollectionTable;
import org.jmouse.query.sql.mapping.JoinMapping;
import org.jmouse.query.sql.mapping.JoinedTable;
import org.jmouse.query.sql.mapping.ColumnMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a parsed {@code source { }} block into the objects a compiler already understands.
 *
 * <h2>⚠️ It produces exactly what a Java mapping produces, and nothing else</h2>
 *
 * <p>A {@link QueryTarget}, a {@link QuerySchema} and an {@link AttributeMapping} — the same three a
 * product hands over when it configures itself in code. So <strong>nothing downstream learns that files
 * are involved</strong>: the checker, the compiler and every backend are untouched by this feature
 * existing, and a product can declare some sources in a file and others in Java without the two being
 * different kinds of thing.</p>
 *
 * <p>That is the test of whether a declarative layer was added in the right place. If it had needed a
 * new interface, or a flag, or a branch anywhere below it, it would have been sitting too deep.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourceLoader {

    private SourceLoader() {
    }

    /**
     * Every source a document declares.
     *
     * @param document a parsed {@code .jmq} document
     * @return the sources, ready to register
     */
    public static List<QuerySource> load(QueryDocumentNode document) {
        List<QuerySource> loaded = new ArrayList<>();

        for (SourceNode declared : document.getSources()) {
            loaded.add(load(declared));
        }

        return loaded;
    }

    /**
     * One source.
     *
     * @param declared a parsed {@code source} block
     * @return the target, schema and mapping it describes
     */
    public static QuerySource load(SourceNode declared) {
        QueryTarget target = new QueryTarget(
                declared.getName(), declared.getTable(), declared.getAlias(), declared.getKey());

        Map<String, QueryAttribute> attributes = new LinkedHashMap<>();

        for (AttributeNode attribute : declared.getAttributes()) {
            attributes.put(attribute.getName(), new QueryAttribute(
                    attribute.getName(),
                    attribute.getSource(),
                    type(attribute.getType()),
                    access(attribute.getAccess())));
        }

        return new QuerySource(target, schema(attributes), mapping(declared), membership(declared));
    }

    /**
     * The other direction: a source built in Java, written back out as the block somebody would have typed.
     *
     * <h2>⚠️ It returns a NODE, and rendering it is somebody else's job</h2>
     *
     * <p>{@link org.jmouse.el.translate.Translator} is the one seam out of the tree — into SQL for a
     * vendor, into a pipeline over rows, or back into jMQ. So this stops at the tree, and the text comes
     * from {@link org.jmouse.query.translate.JmqTranslator} like every other rendering. Producing the
     * string here would be a <strong>second writer</strong> for a language that already has one, and two
     * writers of one language agree until the day only one of them is taught a new spelling.</p>
     *
     * <p>It lives beside {@link #load(SourceNode)} because they are inverses, and inverses that drift are
     * only discovered by whoever round-trips a document and gets a different one back.</p>
     *
     * <h2>⚠️ Why a Java-built source is worth writing out at all</h2>
     *
     * <p>Every product here builds its sources in Java rather than declaring them in a file, and for one
     * of them that is not a preference: what a query may name depends on fields somebody creates on a
     * screen, and a file written in advance cannot mention a field that does not exist yet. The
     * declaration exists — it is simply written nowhere a person can read. This writes it down.</p>
     *
     * <h2>⚠️ What cannot be recovered, and it stays silent rather than guessing</h2>
     *
     * <p>An {@code AttributeMapping} is a <strong>function</strong>, not data: which side table a bag
     * lives in and which columns link and carry it are closed over inside a lambda, so no reading of a
     * {@link QuerySource} recovers the {@code bag} line that would have declared them. The attribute's
     * <em>access</em> survives, because the schema carries it; the table behind it does not. Inventing a
     * plausible one would be worse than omitting it — the result would parse, read as authoritative, and
     * name a table that may not exist.</p>
     *
     * @param source what a product assembled in Java
     * @return the same declaration as a tree, ready for a translator
     */
    public static SourceNode declare(QuerySource source) {
        QueryTarget target   = source.target();
        SourceNode  declared = new SourceNode();

        declared.setName(target.name());
        declared.setStructure(target.name());
        declared.setTable(target.table());
        declared.setAlias(target.alias());
        declared.setKey(target.key());

        for (QueryAttribute attribute : source.schema().attributes()) {
            AttributeNode written = new AttributeNode();

            written.setName(attribute.name());
            written.setSource(attribute.source() == null ? attribute.name() : attribute.source());
            written.setType(written(attribute.type()));
            written.setAccess(written(attribute.access()));

            declared.addAttribute(written);
        }

        return declared;
    }

    /**
     * ⚠️ {@code unknown} is written as itself rather than softened to {@code text}. It is the schema
     * declining to promise anything, and it is why a converter is needed at all — a projection that hid
     * it would describe a source on which comparisons behave differently from the one it came from.
     */
    private static String written(QueryType type) {
        return (type == null ? QueryType.UNKNOWN : type).name().toLowerCase();
    }

    /**
     * ⚠️ Spelled by hand rather than lower-casing the constant, because one of the four does not match:
     * {@link QueryAttribute.Access#JOINED} is written {@code join}. {@code joined} parses — as
     * {@code column}, silently, via the reader's default — so the round trip would lose a join and
     * describe a source that reads the wrong table.
     */
    private static String written(QueryAttribute.Access access) {
        return switch (access == null ? QueryAttribute.Access.COLUMN : access) {
            case BAG -> "bag";
            case JOINED -> "join";
            case COLLECTION -> "collection";
            case COLUMN -> "column";
        };
    }

    /**
     * ⚠️ A source declaring bag attributes and no {@code bag} line is refused <strong>when it is
     * used</strong>, by {@link AttributeMappings#columnsOnly()}, naming the attribute. Refusing at load
     * time instead would stop a product whose file describes several sources because one of them is
     * incomplete — and the message would arrive without the attribute that caused it.
     */
    private static AttributeMapping mapping(SourceNode declared) {
        Optional<BagNode> bag = declared.getBag();

        AttributeMapping bagged = bag
                .<AttributeMapping>map(table -> BagMapping.of(new BagTable(
                        table.getTable(), table.getForeignKey(),
                        table.getKeyColumn(), table.getValueColumn())))
                .orElse(null);

        List<JoinedTable> joined = declared.getJoins().stream()
                .map(join -> new JoinedTable(
                        join.getTable(), join.getLocalColumn(), join.getForeignColumn()))
                .toList();

        if (bagged == null && joined.isEmpty()) {
            return AttributeMappings.columnsOnly();
        }

        return AttributeMappings.byAccess(
                ColumnMapping.qualified(),
                bagged == null ? AttributeMappings.refusing("a bag") : bagged,
                new JoinMapping(joined));
    }

    /**
     * ⚠️ Built even when the source declares no collection, and then it refuses by name. A schema saying
     * an attribute is a collection while nothing says where its items live is a misconfiguration, and the
     * honest answer names the attribute rather than letting the question reach something that answers.
     */
    private static MembershipMapping membership(SourceNode declared) {
        return new CollectionMapping(declared.getCollections().stream()
                .map(collection -> new CollectionTable(
                        collection.getTable(), collection.getForeignKey(), collection.getValueColumn()))
                .toList());
    }

    /**
     * ⚠️ {@code column} for anything unrecognised, and the parser is what makes that safe: it accepts
     * only the four words, so nothing unrecognised can reach here from a document.
     */
    private static QueryAttribute.Access access(String written) {
        return switch (written == null ? "column" : written) {
            case "bag" -> QueryAttribute.Access.BAG;
            case "join" -> QueryAttribute.Access.JOINED;
            case "collection" -> QueryAttribute.Access.COLLECTION;
            default -> QueryAttribute.Access.COLUMN;
        };
    }

    /**
     * ⚠️ {@code unknown} is the honest default for anything unrecognised, and it is the <em>strict</em>
     * one: an attribute of unknown kind refuses an ordered comparison until a converter is given.
     * Defaulting to {@code text} would be more permissive and would answer {@code "900" > "1000"} with
     * true.
     */
    private static QueryType type(String written) {
        return switch (written) {
            case "text" -> QueryType.TEXT;
            case "number" -> QueryType.NUMBER;
            case "boolean" -> QueryType.BOOLEAN;
            case "temporal" -> QueryType.TEMPORAL;
            default -> QueryType.UNKNOWN;
        };
    }

    private static QuerySchema schema(Map<String, QueryAttribute> attributes) {
        return new QuerySchema() {

            @Override
            public Optional<QueryAttribute> attribute(String name) {
                return Optional.ofNullable(attributes.get(name));
            }

            @Override
            public Collection<QueryAttribute> attributes() {
                return attributes.values();
            }
        };
    }
}
