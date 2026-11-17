package org.jmouse.json.syntax;

import org.jmouse.el.StringSource;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.json.JsonSyntaxException;
import org.jmouse.json.node.JsonNode;

/**
 * 🧾 JSON read on the jME engine: JSON's own {@link JsonSplitter} by the engine's splitter contract, the engine's tokenizer,
 * {@link JsonRecognizer}'s tokens, and the parsers of {@link JsonExtension}. Built once; reading
 * keeps no state, so one reader serves every caller.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonReader {

    private final Lexer         lexer;
    private final ParserContext context;

    public JsonReader() {
        StandardExtensionContainer extensions = new StandardExtensionContainer();
        extensions.importExtension(new JsonExtension());
        this.lexer = new DefaultLexer(new DefaultTokenizer(new JsonSplitter(), new JsonRecognizer()));
        this.context = new DefaultParserContext(extensions);
    }

    /** The value a JSON text writes. */
    public Object read(String text) {
        return parse(text).value();
    }

    /** The tree of a JSON text: one value, nothing but space around it. */
    public JsonNode parse(String text) {
        if (text == null || text.isBlank()) {
            throw new JsonSyntaxException("a text holds one value, and this one holds none");
        }

        TokenCursor cursor = lexer.tokenize(new StringSource("JSON", text));
        cursor.consumeIf(BasicToken.T_SOL);

        JsonNode value = (JsonNode) context.getParser(JsonValueParser.class).parse(cursor, context);

        JsonParsing.skipLines(cursor);
        if (!cursor.isCurrent(BasicToken.T_EOL)) {
            throw new JsonSyntaxException("the end of the text after its one value", cursor.peek());
        }

        return value;
    }
}
