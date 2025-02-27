package org.jmouse.template.lexer;

import org.jmouse.template.lexer.recognizer.CompositeRecognizer;
import org.jmouse.template.lexer.recognizer.EnumTokenRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link Tokenizer} that processes text and produces a list of token entries.
 *
 * <p>This tokenizer utilizes a raw text splitter and a composite recognizer to classify tokens based on
 * predefined token sets.</p>
 *
 * <pre>{@code
 * Tokenizer<Token.Entry> tokenizer = new DefaultTokenizer();
 * List<Token.Entry> tokens = tokenizer.tokenize("Hello {{name}}!");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class DefaultTokenizer implements Tokenizer<Token.Entry, Tokenizable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTokenizer.class);

    private final RawSplitter         splitter = new RawSplitter();
    private final CompositeRecognizer recognizer;

    /**
     * Constructs a {@code DefaultTokenizer} with predefined token recognizers.
     * Registers standard and template tokens with specific priority values.
     */
    public DefaultTokenizer() {
        this.recognizer = new CompositeRecognizer();
        this.recognizer.addRecognizer(new EnumTokenRecognizer<>(StandardToken.class, 200));
        this.recognizer.addRecognizer(new EnumTokenRecognizer<>(TemplateToken.class, 100));
    }

    /**
     * Tokenizes the given text into a list of {@link Token.Entry}.
     *
     * @param text the input character sequence to tokenize
     * @return a list of tokenized entries
     */
    @Override
    public List<Token.Entry> tokenize(Tokenizable text) {
        List<Token.Entry> tokens    = new ArrayList<>();
        List<RawToken>    rawTokens = splitter.split(text, 0, text.length());
        int               counter   = 0;

        // Special tokens marking the start and end of a line
        RawToken sol = new RawToken("BEFORE-OF-LINE", Integer.MIN_VALUE, RawToken.Type.UNKNOWN);
        RawToken eol = new RawToken("END-OF-LINE", Integer.MAX_VALUE, RawToken.Type.UNKNOWN);

        tokens.add(entry(StandardToken.T_SOL, sol, counter));

        LOGGER.info("Raw tokens '{}' acquired", rawTokens.size());

        for (RawToken rawToken : rawTokens) {
            RawToken.Type type       = rawToken.type();
            String        tokenValue = rawToken.token().trim();

            // Determine token entry based on the type
            Token.Entry entry = switch (type) {
                case STRING -> entry(StandardToken.T_STRING, rawToken, counter);
                case OPEN_TAG, CLOSE_TAG, IDENTIFIER, OPERATOR, UNKNOWN -> {
                    // Try to recognize the token using registered recognizers
                    Optional<Token> optional = recognizer.recognize(tokenValue);
                    Token           token    = StandardToken.T_IDENTIFIER;

                    if (optional.isPresent()) {
                        token = optional.get();
                    } else if (type == RawToken.Type.UNKNOWN) {
                        token = StandardToken.T_UNKNOWN;
                    }

                    yield entry(token, rawToken, counter);
                }
                case RAW_TEXT -> entry(TemplateToken.T_RAW_TEXT, rawToken, counter);
                case NUMBER -> {
                    // Detect integer and floating-point numbers
                    Token token = StandardToken.T_INT;

                    if (rawToken.token().matches(Syntax.FRACTIONAL_NUMBER)) {
                        token = StandardToken.T_FLOAT;
                    }

                    yield entry(token, rawToken, counter);
                }
            };

            text.entry(rawToken.offset(), rawToken.length(), entry.token());

            tokens.add(entry);
            counter++;
        }

        tokens.add(entry(StandardToken.T_EOL, eol, counter));

        LOGGER.info("Tokenized '{}' tokens", tokens.size());

        return tokens;
    }

    /**
     * Creates a token entry from the given token type and raw token.
     *
     * @param type    the token type
     * @param token   the raw token data
     * @param ordinal the ordinal offset of the token
     * @return a new token entry
     */
    private Token.Entry entry(Token type, RawToken token, int ordinal) {
        return Token.Entry.of(type, token.token(), token.offset(), ordinal);
    }
}
