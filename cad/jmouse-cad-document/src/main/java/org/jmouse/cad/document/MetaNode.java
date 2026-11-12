package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockNode;
import org.jmouse.el.grammar.Nodes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code meta { … }} — what describes a document without changing what it means: its summary, its category, its tags,
 * its links, and any field a curator writes ({@code supply_range 4.5 V .. 16 V}), each kept as written.
 */
public class MetaNode extends BlockNode {

    /** {@code summary "…"}: the sentence a list shows under the title. */
    public static final String SUMMARY = "summary";
    /** {@code category circuits}: the section of the catalogue it is filed in. */
    public static final String CATEGORY = "category";
    /** {@code tags electronics, footprint}: the words it is found by. */
    public static final String TAGS = "tags";
    /** {@code see_also lm324, tl072}: documents a curator links it to. */
    public static final String SEE_ALSO = "see_also";
    /** {@code featured yes}: shown first in its section. */
    public static final String FEATURED = "featured";
    /** {@code code "FP.AXIAL:R0207-P10.16-H"}: its technical code. */
    public static final String CODE = "code";
    /** {@code links: { "Datasheet" "https://…" }}: where it is spoken of elsewhere, a label and an address on each line. */
    public static final String LINKS = "links";
    /** {@code source "…"}: where it was taken from. */
    public static final String SOURCE = "source";
    /** {@code authors "…"}: who holds it. */
    public static final String AUTHORS = "authors";
    /** {@code license "…"}: the licence it is under. */
    public static final String LICENSE = "license";
    /**
     * {@code status draft}: where a catalogue document stands ({@link DocumentStatus}) — a draft, or archived —; a document that
     * says nothing is published.
     */
    public static final String STATUS = "status";
    /** {@code order 1}: where a category stands among its siblings. */
    public static final String ORDER = "order";
    /** {@code designator U}: what a part of it is called on a sheet before its number. */
    public static final String DESIGNATOR = "designator";

    /** The parameter a component's value is — {@code value U_output} —: what a schematic prints beside its designator. */
    public static final String VALUE = "value";

    private static final String LIST_SEPARATOR = ",";

    /** A line of {@code links}, as it is written: two texts in quotes. */
    private static final Pattern LINK_LINE = Pattern.compile("\"([^\"]*)\"\\s*,?\\s*\"([^\"]*)\"");

    /** A line of {@code links}: what it is called, and where it is. */
    public record Link(String label, String address) {
    }

    /**
     * The meta a document made by a program starts with — a converted, a generated, an imported one —: what it is, its
     * technical code, where it is filed and its tags, each left out where none is given.
     *
     * @param code     its technical code ({@code FP.CHIP:R0805}), or null
     * @param category the category it is filed in, or null
     */
    public static MetaNode filed(String summary, String code, String category, List<String> tags) {
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.of(SUMMARY, Nodes.text(summary)));
        if (code != null) {
            meta.addExpression(MetaLineNode.of(CODE, Nodes.text(code)));
        }
        if (category != null) {
            meta.addExpression(MetaLineNode.of(CATEGORY, Nodes.word(category)));
        }
        if (!tags.isEmpty()) {
            meta.addExpression(MetaLineNode.of(TAGS, Nodes.words(tags)));
        }
        return meta;
    }

    @Override
    public String word() {
        return MetaParser.WORD;
    }

    /** Its {@code links}, in order; a line that is not a label and an address is no link. */
    public List<Link> links() {
        return line(LINKS).map(MetaLineNode::group).orElse(List.of()).stream()
                .map(LINK_LINE::matcher).filter(Matcher::matches)
                .map(matcher -> new Link(matcher.group(1), matcher.group(2))).toList();
    }

    public List<MetaLineNode> lines() {
        return getExpressions().stream().filter(MetaLineNode.class::isInstance).map(MetaLineNode.class::cast).toList();
    }

    public Optional<MetaLineNode> line(String key) {
        return lines().stream().filter(line -> line.key().equals(key)).findFirst();
    }

    /** A field's text: {@code summary "…"} without its quotes. */
    public Optional<String> text(String key) {
        return line(key).map(MetaLineNode::text);
    }

    /** A field written as a list — {@code tags electronics, footprint} —, each word once, in order; empty when absent. */
    public List<String> words(String key) {
        return text(key).stream()
                .flatMap(written -> Arrays.stream(written.split(LIST_SEPARATOR)))
                .map(String::strip)
                .filter(word -> !word.isEmpty())
                .distinct()
                .toList();
    }
}
