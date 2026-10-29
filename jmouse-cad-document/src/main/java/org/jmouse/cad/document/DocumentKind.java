package org.jmouse.cad.document;

import org.jmouse.el.parser.Parser;

import java.util.Optional;
import java.util.Set;

/**
 * A kind of document — a symbol, a footprint, a circuit —: the word its head opens with, the blocks its body may hold,
 * and, for a drawn kind, the parser of the lines its body is drawn with. The language knows no kind of its own; a
 * {@link DocumentModule} brings each.
 *
 * @param word       {@code symbol "Resistor" { … }}; a file of this kind is named {@code <slug>.<word>.jm}
 * @param blocks     the blocks its body may hold, besides {@code meta} which every document may
 * @param repeatable the blocks of those it may hold more than once
 * @param lines      the parser of a drawn kind's body lines — a symbol's {@code pin} and {@code path}, a footprint's
 *                   {@code pad} —, which stand at the top of its body; null for a kind whose body is blocks only
 */
public record DocumentKind(String word, Set<String> blocks, Set<String> repeatable, Class<? extends Parser> lines) {

    public DocumentKind {
        blocks = Set.copyOf(blocks);
        repeatable = Set.copyOf(repeatable);
    }

    public static DocumentKind of(String word, String... blocks) {
        return new DocumentKind(word, Set.of(blocks), Set.of(), null);
    }

    /** The same kind, with some of its blocks written any number of times. */
    public DocumentKind repeating(String... blocks) {
        return new DocumentKind(word, this.blocks, Set.of(blocks), lines);
    }

    /** The same kind, drawn: its body's lines read by this parser, which its module registers. */
    public DocumentKind drawnBy(Class<? extends Parser> lines) {
        return new DocumentKind(word, blocks, repeatable, lines);
    }

    public Optional<Class<? extends Parser>> bodyLines() {
        return Optional.ofNullable(lines);
    }

    public boolean holds(String block) {
        return MetaParser.WORD.equals(block) || blocks.contains(block);
    }

    public boolean repeats(String block) {
        return repeatable.contains(block);
    }

    /** {@code .symbol.jm} */
    public String fileSuffix() {
        return "." + word + DocumentInput.FILE_SUFFIX;
    }
}
