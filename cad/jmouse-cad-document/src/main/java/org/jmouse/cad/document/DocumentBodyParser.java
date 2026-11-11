package org.jmouse.cad.document;

import java.util.Optional;
import java.util.Map;
import org.jmouse.el.grammar.LineReader;
import org.jmouse.el.grammar.ClauseBlockParser;
import org.jmouse.el.grammar.BlockParser;
import org.jmouse.el.grammar.GrammarRefusal;

import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The body of a document: its declarations and its blocks. A statement here is never the context's to choose — the
 * block's word in a document of its kind says which parser reads it ({@link DocumentGrammar}), so a word of the
 * language stays free as a name in a jMS body read through the same context.
 */
public class DocumentBodyParser extends StatementsParser {

    private final DocumentGrammar   grammar;
    private final DeclarationParser declarations = new DeclarationParser();
    private DocumentKind kind;
    private Set<String>  written = new HashSet<>();

    public DocumentBodyParser(DocumentGrammar grammar) {
        this.grammar = grammar;
    }

    /** The body about to be read is a document of this kind. */
    void begin(DocumentKind kind) {
        this.kind = kind;
        this.written = new HashSet<>();
    }

    @Override
    protected Expression statement(TokenCursor cursor, ParserContext context) {
        Token word = cursor.current();
        if (word.type() == BasicToken.T_STRING) {
            throw GrammarRefusal.at(cursor, word, "a text in quotes cannot start a line here");
        }
        if (DocumentMatcher.document(grammar).matches(cursor)) {
            throw GrammarRefusal.at(cursor, word, "a " + kind.word() + " holds no other document: one document is one file");
        }
        if (declarations.declares(cursor)) {
            return (Expression) declarations.parse(cursor, context);
        }
        String name = word.value();
        // A drawn kind's lines stand at the top of its body; a word it holds as a block, opened by '{', is a block still.
        boolean opensBlock = kind.holds(name) && cursor.isNext(BasicToken.T_OPEN_CURLY);
        if (!opensBlock && kind.bodyLines().isPresent()) {
            return (Expression) context.getParser(kind.bodyLines().get()).parse(cursor, context);
        }
        // A term whose block was left out — parameter:R(…) — is read here; the document gathers it into its block.
        Optional<Map.Entry<ClauseBlockParser, LineReader>> gathered = grammar.gathering(cursor, kind);
        if (gathered.isPresent()) {
            return gathered.get().getValue().readTerm(cursor, context);
        }
        if (!kind.holds(name)) {
            throw GrammarRefusal.at(cursor, word, "'" + name + "' is not a block of a " + kind.word() + "; it holds "
                    + kind.blocks().stream().sorted().collect(Collectors.joining(", ")));
        }
        BlockParser<?> block = grammar.block(cursor, kind)
                .orElseThrow(() -> GrammarRefusal.at(cursor, word, "'" + name + "' is a block of a " + kind.word()
                        + " no module of this language reads"));
        if (!kind.repeats(name) && !written.add(name)) {
            throw GrammarRefusal.at(cursor, word, "'" + name + "' is written twice");
        }
        return (Expression) block.parse(cursor, context);
    }
}
