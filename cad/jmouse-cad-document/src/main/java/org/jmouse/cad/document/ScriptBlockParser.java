package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockParser;
import org.jmouse.el.grammar.GrammarRefusal;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;

/**
 * {@code word { jMS }}: a block whose body jMS reads with its own statement reader, through the same context — the
 * engine's default for a body. The body is kept as written as well, since that is what is written back.
 */
public class ScriptBlockParser extends BlockParser<ScriptBlockNode> {

    private final String     word;
    private final Token.Type token;

    /** @param token what the lexer reads the word as: jMS keeps {@code behaviour} as a keyword of its own */
    public ScriptBlockParser(String word, Token.Type token) {
        this.word = word;
        this.token = token;
    }

    @Override
    protected Token.Type token() {
        return token;
    }

    @Override
    public String word() {
        return word;
    }

    @Override
    protected ScriptBlockNode node(TokenCursor cursor, ParserContext context) {
        return new ScriptBlockNode(word);
    }

    @Override
    protected void parseBlock(TokenCursor cursor, ScriptBlockNode node, ParserContext context) {
        Token open = cursor.current();
        try {
            super.parseBlock(cursor, node, context);
        } catch (GrammarRefusal refusal) {
            throw refusal;
        } catch (RuntimeException failure) {
            throw GrammarRefusal.at(cursor, open, "in '" + word + "': " + failure.getMessage());
        }
        node.written(DocumentSource.of(cursor).between(open, cursor.lookAt(-1)));
    }
}
