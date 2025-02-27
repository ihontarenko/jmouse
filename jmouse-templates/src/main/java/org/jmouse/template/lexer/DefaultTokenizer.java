package org.jmouse.template.lexer;

import org.jmouse.template.StringSource;
import org.jmouse.template.lexer.recognizer.CompositeRecognizer;
import org.jmouse.template.lexer.recognizer.EnumTokenRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.jmouse.template.lexer.RawToken.Type.UNKNOWN;

/**
 * Default implementation of {@link Tokenizer} that processes text and produces a list of type entries.
 *
 * <p>This tokenizer utilizes a raw text splitter and a composite recognizer to classify tokens based on
 * predefined type sets.</p>
 *
 * <pre>{@code
 * Tokenizer<TokenType.Entry> tokenizer = new DefaultTokenizer();
 * List<TokenType.Entry> tokens = tokenizer.tokenize("Hello {{name}}!");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class DefaultTokenizer implements Tokenizer<Token, StringSource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTokenizer.class);

    private final RawSplitter         splitter = new RawSplitter();
    private final CompositeRecognizer recognizer;

    /**
     * Constructs a {@code DefaultTokenizer} with predefined type recognizers.
     * Registers standard and template tokens with specific priority values.
     */
    public DefaultTokenizer() {
        this.recognizer = new CompositeRecognizer();
        this.recognizer.addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 200));
        this.recognizer.addRecognizer(new EnumTokenRecognizer<>(TemplateToken.class, 100));
    }

    /**
     * Tokenizes the given text into a list of {@link Token}.
     *
     * @param text the input character sequence to tokenize
     * @return a list of tokenized entries
     */
    @Override
    public List<Token> tokenize(StringSource text) {
        List<Token>       tokens      = new ArrayList<>();
        List<RawToken>    rawTokens   = splitter.split(text, 0, text.length());
        int               counter     = 0;
        TokenizableString tokenizable = text.getTokenizable();

        // Special tokens marking the start and end of a line
        RawToken sol = new RawToken("BEFORE-OF-LINE", -1, MIN_VALUE, UNKNOWN);
        RawToken eol = new RawToken("END-OF-LINE", -1, MAX_VALUE, UNKNOWN);

        tokens.add(entry(BasicToken.T_SOL, sol, counter));

        LOGGER.info("Raw tokens '{}' acquired", rawTokens.size());

        for (RawToken rawToken : rawTokens) {
            RawToken.Type type       = rawToken.type();
            String        tokenValue = rawToken.value().trim();

            // Determine type entry based on the type
            Token token = switch (type) {
                case STRING -> entry(BasicToken.T_STRING, rawToken, counter);
                case OPEN_TAG, CLOSE_TAG, IDENTIFIER, OPERATOR, UNKNOWN -> {
                    // Try to recognize the type using registered recognizers
                    Optional<Token.Type> optional = recognizer.recognize(tokenValue);
                    Token.Type           tokenType    = BasicToken.T_IDENTIFIER;

                    if (optional.isPresent()) {
                        tokenType = optional.get();
                    } else if (type == UNKNOWN) {
                        tokenType = BasicToken.T_UNKNOWN;
                    }

                    yield entry(tokenType, rawToken, counter);
                }
                case RAW_TEXT -> entry(TemplateToken.T_RAW_TEXT, rawToken, counter);
                case NUMBER -> {
                    // Detect integer and floating-point numbers
                    Token.Type tokenType = BasicToken.T_INT;

                    if (rawToken.value().matches(Syntax.FRACTIONAL_NUMBER)) {
                        tokenType = BasicToken.T_FLOAT;
                    }

                    yield entry(tokenType, rawToken, counter);
                }
            };

            tokenizable.entry(rawToken.offset(), rawToken.length(), token.type());

            tokens.add(token);

            counter++;
        }

        tokens.add(entry(BasicToken.T_EOL, eol, counter));

        LOGGER.info("Tokenized '{}' tokens", tokens.size());

        return tokens;
    }

    /**
     * Creates a type entry from the given type and raw type.
     *
     * @param type    the type
     * @param token   the raw type data
     * @param ordinal the ordinal offset of the type
     * @return a new type entry
     */
    private Token entry(Token.Type type, RawToken token, int ordinal) {
        return new Token(token.value(), type, ordinal, token.offset(), token.line());
    }
}
