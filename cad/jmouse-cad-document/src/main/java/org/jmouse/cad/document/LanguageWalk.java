package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockParser;
import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.ClauseLineParser;
import org.jmouse.el.grammar.GrammarLinesParser;
import org.jmouse.el.grammar.LineReader;
import org.jmouse.el.grammar.LineSet;
import org.jmouse.el.parser.Parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The jM language as its modules make it, walked once: every kind, the blocks a body may hold, the lines a drawn kind's
 * body is drawn with, the script blocks, and every word a term may open with. What describes the language — its TextMate
 * grammar, the schema an editor builds its forms from — reads this, so the descriptions cannot walk it differently.
 *
 * @param kinds   every kind, by its word
 * @param blocks  every block of lines, by its word, in the order the modules bring them
 * @param scripts the word of every block whose body is jMS
 * @param drawn   the lines each drawn kind's body is drawn with, by the kind's word
 * @param terms   every word a term opens with: a block's, its header's, a line's
 */
public record LanguageWalk(Map<String, DocumentKind> kinds, Map<String, ClauseBlockParser> blocks, Set<String> scripts,
                           Map<String, LineSet> drawn, Set<String> terms) {

    public LanguageWalk {
        kinds = Collections.unmodifiableMap(new TreeMap<>(kinds));
        blocks = Collections.unmodifiableMap(new LinkedHashMap<>(blocks));
        scripts = Collections.unmodifiableSet(new TreeSet<>(scripts));
        drawn = Collections.unmodifiableMap(new LinkedHashMap<>(drawn));
        terms = Collections.unmodifiableSet(new TreeSet<>(terms));
    }

    /** The language the modules make, walked. */
    public static LanguageWalk of(List<DocumentModule> modules) {
        Walking walking = new Walking();
        for (DocumentModule module : modules) {
            for (DocumentKind kind : module.kinds()) {
                walking.kinds.put(kind.word(), kind);
                kind.bodyLines().ifPresent(lines -> drawnLines(module, lines).ifPresent(set -> {
                    walking.drawn.put(kind.word(), set);
                    walking.walk(set);
                }));
            }
            for (BlockParser<?> block : module.blocks()) {
                walking.walk(block);
            }
        }
        return new LanguageWalk(walking.kinds, walking.blocks, walking.scripts, walking.drawn, walking.terms);
    }

    private static Optional<LineSet> drawnLines(DocumentModule module, Class<? extends Parser> type) {
        return module.getParsers().stream().filter(type::isInstance).filter(GrammarLinesParser.class::isInstance)
                .map(parser -> ((GrammarLinesParser) parser).lineSet()).findFirst();
    }

    /** What the walk has met so far. */
    private static final class Walking {

        private final Map<String, DocumentKind>      kinds   = new TreeMap<>();
        private final Map<String, ClauseBlockParser> blocks  = new LinkedHashMap<>();
        private final Set<String>                    scripts = new TreeSet<>();
        private final Map<String, LineSet>           drawn   = new LinkedHashMap<>();
        private final Set<String>                    terms   = new TreeSet<>();

        private void walk(Object reader) {
            switch (reader) {
                case ClauseBlockParser block -> {
                    if (blocks.putIfAbsent(block.word(), block) != null) {
                        return;
                    }
                    terms.add(block.word());
                    block.header().ifPresent(header -> terms.add(header.termWord()));
                    if (block.lines() != null) {
                        walk(block.lines());
                    }
                }
                case ScriptBlockParser script -> scripts.add(script.word());
                case ClauseLineParser line -> {
                    if (line.grammar().termWord() != null) {
                        terms.add(line.grammar().termWord());
                    }
                }
                default -> {
                }
            }
        }

        private void walk(LineSet lines) {
            for (LineReader reader : lines.readers()) {
                for (LineReader alternative : reader.alternatives()) {
                    walk(alternative);
                }
            }
        }
    }
}
