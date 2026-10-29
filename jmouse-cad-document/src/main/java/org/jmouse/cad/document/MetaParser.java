package org.jmouse.cad.document;

import org.jmouse.el.grammar.BlockParser;
import org.jmouse.el.grammar.LinesParser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;

/** {@code meta { … }}: every document may hold it, whatever modules the language is made of. */
public class MetaParser extends BlockParser<MetaNode> {

    public static final String WORD = "meta";

    @Override
    public String word() {
        return WORD;
    }

    @Override
    protected MetaNode node(TokenCursor cursor, ParserContext context) {
        return new MetaNode();
    }

    @Override
    protected Class<? extends Parser> statementsParser() {
        return MetaLinesParser.class;
    }

    /** The lines of {@code meta}, each a field. */
    public static class MetaLinesParser extends LinesParser {

        @Override
        protected Class<? extends Parser> lineParser() {
            return MetaLineParser.class;
        }
    }
}
