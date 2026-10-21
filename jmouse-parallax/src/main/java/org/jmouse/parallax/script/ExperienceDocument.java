package org.jmouse.parallax.script;

import org.jmouse.core.text.Slugifier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One jMX document: {@code <kind> "Title" [as slug] [type role] { … }}.
 *
 * <p>The head is identity only. What changes computation — the base a component specializes, the units of a package,
 * the documents it uses — sits at the top level of the body; everything descriptive is in {@link #meta()}.</p>
 *
 * @param slug     the stable address: {@code as slug} when written, otherwise derived from the title
 * @param type     the role within the kind — {@code resistor}, {@code timer}, {@code flip-flop} for a component
 * @param base     the component this one specializes and inherits the physics of ({@code extends diode})
 * @param units    the units of a multi-unit package ({@code units A, B})
 * @param uses     the documents this one pulls in ({@code use cmos-hc, smooth-transitions}): a logic family, a jMF
 *                 library, a dependency — which, is decided by the used document's kind
 * @param line     the line the document opens on
 */
public record ExperienceDocument(Kind kind, String title, String slug, Optional<String> type, Meta meta,
                                 Optional<String> base, List<String> units, List<String> uses, List<Block> blocks,
                                 int line) {

    public ExperienceDocument {
        units = List.copyOf(units);
        uses = List.copyOf(uses);
        blocks = List.copyOf(blocks);
    }

    /** The first block of a type, if the document has one. */
    public <T extends Block> Optional<T> block(Class<T> type) {
        return blocks.stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    /** Every block of a type, in the order written — a document may hold several {@code expect} or {@code analysis}. */
    public <T extends Block> List<T> blocks(Class<T> type) {
        return blocks.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public List<Block.Parameter> parameters() {
        return block(Block.Parameters.class).map(Block.Parameters::parameters).orElse(List.of());
    }

    public List<Block.Output> outputs() {
        return block(Block.Outputs.class).map(Block.Outputs::outputs).orElse(List.of());
    }

    /** What this document can do on its own; a circuit's parts may add more once they are resolved. */
    public Set<Capability> capabilities() {
        return Capabilities.of(this);
    }

    /**
     * A slug from a title: lowercase ASCII words joined by hyphens, by the rule every jmouse tree mints its slugs with —
     * Cyrillic transliterated, marks dropped, a superscript digit kept as a digit. A document whose head writes
     * {@code as slug} keeps that one; this answers only for a document that writes none.
     */
    public static String slugOf(String title) {
        return SLUGS.slug(title);
    }

    /** The slug rule, falling back to {@code untitled} when nothing of a title can stand in an address. */
    private static final Slugifier SLUGS = Slugifier.standard().fallingBackTo("untitled");
}
