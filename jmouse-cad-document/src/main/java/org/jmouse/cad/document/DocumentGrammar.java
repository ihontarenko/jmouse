package org.jmouse.cad.document;

import org.jmouse.el.grammar.LineReader;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.ClauseLineParser;
import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.BlockParser;

import org.jmouse.el.lexer.TokenCursor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The language as the modules it is made of say it: every kind of document by its word, and every block a body may
 * hold. Built once per language; a reading only asks it.
 */
public final class DocumentGrammar {

    private final Map<String, DocumentKind>     kinds  = new LinkedHashMap<>();
    private final List<BlockParser<?>> blocks = new ArrayList<>();

    DocumentGrammar(List<DocumentModule> modules) {
        blocks.add(new MetaParser());
        for (DocumentModule module : modules) {
            for (DocumentKind kind : module.kinds()) {
                if (kinds.putIfAbsent(kind.word(), kind) != null) {
                    throw new IllegalStateException("two modules bring a kind of document called '" + kind.word() + "'");
                }
            }
            blocks.addAll(module.blocks());
        }
    }

    public Optional<DocumentKind> kind(String word) {
        return Optional.ofNullable(kinds.get(word));
    }

    /**
     * The block a term of the body belongs to, when a document is written as terms with the blocks that only gather them
     * left out — {@code parameter:R(…)} is a line of {@code parameters} —, and the reader of that term; empty for any
     * other statement.
     */
    public Optional<Map.Entry<ClauseBlockParser, LineReader>> gathering(TokenCursor cursor, DocumentKind kind) {
        for (BlockParser<?> block : blocks) {
            if (block instanceof ClauseBlockParser clauses && !clauses.headed() && clauses.lines() != null && kind.holds(clauses.word())) {
                for (LineReader reader : clauses.lines().readers()) {
                    if (reader instanceof ClauseLineParser line && line.grammar().isNamed() && reader.opensTerm(cursor)) {
                        return Optional.of(Map.entry(clauses, reader));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** The gathering block a line of this grammar belongs to, in a document of this kind. */
    public Optional<ClauseBlockParser> gatheredBy(LineGrammar line, DocumentKind kind) {
        for (BlockParser<?> block : blocks) {
            if (block instanceof ClauseBlockParser clauses && !clauses.headed() && clauses.lines() != null && kind.holds(clauses.word())
                    && clauses.lines().readers().stream().anyMatch(reader -> reader instanceof ClauseLineParser parser
                    && parser.grammar() == line)) {
                return Optional.of(clauses);
            }
        }
        return Optional.empty();
    }

    /** The words of every kind it reads, for a refusal to list. */
    public String kindWords() {
        return String.join(", ", kinds.keySet());
    }

    /** The parser of the block the cursor stands on, in a document of a kind; empty when none reads it there. */
    Optional<BlockParser<?>> block(TokenCursor cursor, DocumentKind kind) {
        return blocks.stream().filter(block -> kind.holds(block.word()) && block.supports(cursor)).findFirst();
    }

    List<BlockParser<?>> blocks() {
        return List.copyOf(blocks);
    }
}
