package org.jmouse.mapper.el.translate;

import org.jmouse.el.node.Node;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Capability;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.el.translate.Translator;
import org.jmouse.mapper.el.node.AssertionNode;
import org.jmouse.mapper.el.node.FromNode;
import org.jmouse.mapper.el.node.IncludeNode;
import org.jmouse.mapper.el.node.LetNode;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.RefuseNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.RuleNode;
import org.jmouse.mapper.el.node.TargetNode;
import org.jmouse.mapper.el.node.UseNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A mapping document as JSON — the shape, for something that is not the mapper.
 *
 * <h2>⚠️ Why this exists, and it is not the output</h2>
 *
 * <p>It is the <strong>cheap proof the lifted seam survived the trip</strong>. {@code Translator} came
 * out of {@code jmouse-query}, where every implementation renders a query; if it came out shaped like a
 * query rather than like a tree, a destination that is not a query and not source text is where it
 * shows — in a day, against something nobody can argue about, before anything larger is built on it.
 * That is worth more than the JSON is.</p>
 *
 * <p>It happens to also be useful: a {@code .jmm} file readable by a tool, a diff or a screen without
 * teaching any of them the grammar.</p>
 *
 * <h2>⚠️ Nothing reads it back, and nothing will</h2>
 *
 * <p>A reader for this would be a second front end for the language, competing with the one that
 * decides what a mapping means. The machine-readable form of a mapping is {@code .jmm}, which
 * round-trips — see {@link JmmSourceTranslator}.</p>
 *
 * <h2>⚠️ Every construct appears, including the ones a naive walker forgets</h2>
 *
 * <p>{@code always}, {@code include}, {@code when}, {@code refuse}, {@code ignore} and {@code : via(…)}
 * are each written out under their own key — and a rule that was {@code ignore} says so with a flag
 * rather than by having no value, because "no value" and "deliberately left alone" are the two things
 * a reader of this most needs told apart.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmJsonTranslator implements Translator<String> {

    private static final String INDENT     = "  ";
    private static final String LINE_BREAK = "\n";

    private static final Capabilities EVERYTHING = Capabilities.of("json", JmmCapability.every());

    /** The full destination — it writes down whatever tree it is handed. */
    public static final JmmJsonTranslator INSTANCE = new JmmJsonTranslator(EVERYTHING);

    private final Capabilities capabilities;

    private JmmJsonTranslator(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * A destination that writes only some of what a mapping can hold.
     *
     * @param capabilities what it will write — from {@link JmmCapability}
     * @return the narrowed destination
     */
    public static JmmJsonTranslator writing(Capability... capabilities) {
        return new JmmJsonTranslator(Capabilities.of("json", capabilities));
    }

    @Override
    public Capabilities capabilities() {
        return capabilities;
    }

    /**
     * Renders a mapping document as JSON.
     *
     * @param node     a {@link MappingDocumentNode}
     * @param bindings nothing — substituting a caller's value into a rendering of the tree produces a
     *                 document that is correct for one person and wrong for everybody else
     * @return the document as pretty-printed JSON
     * @throws TranslationRefusedException where the tree is not a mapping document, holds a construct
     *                                     this destination does not declare, or came with bindings
     */
    @Override
    public String translate(Node node, Bindings bindings) {
        if (!bindings.isEmpty()) {
            throw new TranslationRefusedException(
                    ("the 'json' translator writes down the tree it is handed and nothing else; these "
                     + "would be silently dropped: %s").formatted(bindings.names()));
        }

        if (!(node instanceof MappingDocumentNode document)) {
            throw new TranslationRefusedException(
                    "the 'json' translator writes a mapping document, and %s is not one"
                            .formatted(node.getClass().getSimpleName()));
        }

        new JmmCapabilityCheck(capabilities).check(document);

        return object(0, List.of(
                field("mapping", string(document.getName())),
                field("imports", imports(1, document)),
                field("fragments", fragments(1, document)),
                field("targets", targets(1, document))));
    }

    /**
     * The types the file named short.
     *
     * @param depth  how far in
     * @param document the parsed file
     * @return the imports as an array
     */
    private String imports(int depth, MappingDocumentNode document) {
        List<String> entries = new ArrayList<>();

        for (Map.Entry<String, UseNode> imported : document.getImports().entrySet()) {
            entries.add(object(depth + 1, List.of(
                    field("name", string(imported.getKey())),
                    field("type", string(imported.getValue().getQualifiedName())))));
        }

        return array(depth, entries);
    }

    /**
     * The rule blocks the file named for reuse.
     *
     * @param depth    how far in
     * @param document the parsed file
     * @return the fragments as an array
     */
    private String fragments(int depth, MappingDocumentNode document) {
        List<String> entries = new ArrayList<>();

        for (Map.Entry<String, RuleBlockNode> fragment : document.getFragments().entrySet()) {
            entries.add(object(depth + 1, List.of(
                    field("name", string(fragment.getKey())),
                    field("rules", rules(depth + 2, fragment.getValue())))));
        }

        return array(depth, entries);
    }

    /**
     * What the file builds.
     *
     * @param depth    how far in
     * @param document the parsed file
     * @return the targets as an array
     */
    private String targets(int depth, MappingDocumentNode document) {
        List<String> entries = new ArrayList<>();

        for (TargetNode target : document.getTargets()) {
            entries.add(object(depth + 1, List.of(
                    field("type", string(target.getTargetType())),
                    field("unmapped", string(target.getUnmapped().name())),
                    field("always", rules(depth + 2, target.getAlways())),
                    field("refusals", refusals(depth + 2, target.getRefusals())),
                    field("sources", sources(depth + 2, target)))));
        }

        return array(depth, entries);
    }

    /**
     * The sources of one target.
     *
     * @param depth  how far in
     * @param target the block
     * @return the sources as an array
     */
    private String sources(int depth, TargetNode target) {
        List<String> entries = new ArrayList<>();

        for (FromNode source : target.getSources()) {
            entries.add(object(depth + 1, List.of(
                    field("type", string(source.getSourceType())),
                    field("conversion", string(source.getConversion())),
                    field("refusals", refusals(depth + 2, source.getRefusal() == null
                            ? List.of() : List.of(source.getRefusal()))),
                    field("rules", rules(depth + 2, source.getRules())))));
        }

        return array(depth, entries);
    }

    /**
     * The assertion blocks that can stop a mapping.
     *
     * @param depth    how far in
     * @param refusals the blocks
     * @return the refusals as an array
     */
    private String refusals(int depth, List<RefuseNode> refusals) {
        List<String> entries = new ArrayList<>();

        for (RefuseNode refusal : refusals) {
            List<String> assertions = new ArrayList<>();

            // ⚠️ Depths are absolute, not relative: an entry of this array sits at depth + 1, so its
            // own fields sit at depth + 2 and a nested array closes there, with its entries at
            // depth + 3. Passing a depth that merely looks plausible renders valid JSON at the wrong
            // indent, which is the one defect a parser will never report.
            for (AssertionNode assertion : refusal.getAssertions()) {
                assertions.add(object(depth + 3, List.of(
                        field("condition", string(assertion.getCondition())),
                        field("message", string(assertion.getMessage())))));
            }

            entries.add(object(depth + 1, List.of(
                    field("subject", string(refusal.getSubject().name())),
                    field("phase", string(refusal.getPhase().name())),
                    field("assertions", array(depth + 2, assertions)))));
        }

        return array(depth, entries);
    }

    /**
     * One block of rules — the same shape wherever it came from.
     *
     * @param depth how far in
     * @param block the rules, which may be {@code null} for a block that declared none
     * @return the block as an object, or {@code null} where there is none
     */
    private String rules(int depth, RuleBlockNode block) {
        if (block == null || block.isEmpty()) {
            return "null";
        }

        List<String> bindings = new ArrayList<>();

        for (LetNode binding : block.getBindings()) {
            bindings.add(object(depth + 2, List.of(
                    field("name", string(binding.getName())),
                    field("expression", string(binding.getExpression())))));
        }

        List<String> written = new ArrayList<>();

        for (RuleNode rule : block.getRules().values()) {
            written.add(object(depth + 2, List.of(
                    field("property", string(rule.getProperty())),
                    field("value", string(rule.getValue())),
                    field("condition", string(rule.getCondition())),
                    field("ignored", String.valueOf(rule.isIgnored())))));
        }

        List<String> includes = new ArrayList<>();

        for (IncludeNode include : block.getIncludes()) {
            includes.add(string(include.getName()));
        }

        return object(depth, List.of(
                field("includes", array(depth + 1, includes)),
                field("bindings", array(depth + 1, bindings)),
                field("rules", array(depth + 1, written))));
    }

    // ── JSON, written by hand because a dependency for this would be the larger cost ──────────────

    /**
     * One {@code "name": value} pair, already rendered.
     *
     * @param name  the key
     * @param value the value, rendered
     * @return the pair
     */
    private String field(String name, String value) {
        return string(name) + ": " + value;
    }

    /**
     * An object, one field per line.
     *
     * @param depth  how far the closing brace sits in
     * @param fields the rendered fields
     * @return the object
     */
    private String object(int depth, List<String> fields) {
        if (fields.isEmpty()) {
            return "{}";
        }

        return "{" + LINE_BREAK
               + INDENT.repeat(depth + 1)
               + String.join("," + LINE_BREAK + INDENT.repeat(depth + 1), fields)
               + LINE_BREAK + INDENT.repeat(depth) + "}";
    }

    /**
     * An array, one entry per line.
     *
     * @param depth   how far the closing bracket sits in
     * @param entries the rendered entries
     * @return the array
     */
    private String array(int depth, List<String> entries) {
        if (entries.isEmpty()) {
            return "[]";
        }

        return "[" + LINE_BREAK
               + INDENT.repeat(depth + 1)
               + String.join("," + LINE_BREAK + INDENT.repeat(depth + 1), entries)
               + LINE_BREAK + INDENT.repeat(depth) + "]";
    }

    /**
     * A string, or {@code null}.
     *
     * <p>⚠️ Unlike the {@code .jmm} lexer, JSON <em>does</em> have escapes — so a value is escaped here
     * rather than having its quote character chosen. The two rules look alike and are not, which is
     * why {@link org.jmouse.el.lexer.support.SourceWriting} is not reused for this.</p>
     *
     * @param value what to write
     * @return the JSON literal
     */
    private String string(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder("\"");

        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append("\\u%04x".formatted((int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }

        return escaped.append('"').toString();
    }
}
