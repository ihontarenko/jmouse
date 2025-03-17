package org.jmouse.el.lexer;

import org.jmouse.el.lexer.recognizer.Recognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.jmouse.el.lexer.RawToken.Type.UNKNOWN;

/**
 * Default implementation of {@link Tokenizer} that processes text and produces a list of type entries.
 *
 * <p>This tokenizer utilizes a raw text splitter and a composite recognizer to classify tokens based on
 * predefined type sets.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class DefaultTokenizer implements Tokenizer<TokenizableSource, Token> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTokenizer.class);

    private final Splitter<List<RawToken>, TokenizableSource> splitter;
    private final Recognizer<Token.Type, RawToken>            recognizer;

    /**
     * Constructs a {@code TemplateTokenizer} with predefined type recognizers.
     * Registers standard and template tokens with specific priority values.
     */
    public DefaultTokenizer(Splitter<List<RawToken>, TokenizableSource> splitter, Recognizer<Token.Type, RawToken> recognizer) {
        this.splitter = splitter;
        this.recognizer = recognizer;
    }

    /**
     * Tokenizes the given source into a list of {@link Token}.
     *
     * @param source the input character sequence to tokenize
     * @return a list of tokenized entries
     */
    @Override
    public List<Token> tokenize(TokenizableSource source) {
        List<Token>    tokens    = new ArrayList<>();
        List<RawToken> rawTokens = splitter.split(source);
        int            counter   = 0;

        // Special tokens marking the start and end of a line
        RawToken sol = new RawToken("BEFORE-OF-LINE", -1, MIN_VALUE, UNKNOWN);
        RawToken eol = new RawToken("END-OF-LINE", -1, MAX_VALUE, UNKNOWN);

        tokens.add(entry(BasicToken.T_SOL, sol, counter));

        LOGGER.info("Raw tokens '{}' acquired", rawTokens.size());

        for (RawToken rawToken : rawTokens) {
            RawToken.Type type       = rawToken.type();

            // Determine type entry based on the type
            Token token = switch (type) {
                case STRING -> entry(BasicToken.T_STRING, rawToken, counter);
                case OPEN_TAG, CLOSE_TAG, IDENTIFIER, OPERATOR, UNKNOWN, RAW_TEXT -> {
                    Token.Type tokenType = BasicToken.T_IDENTIFIER;

                    // Try to recognize the type using registered recognizers
                    Optional<Token.Type> optional = recognizer.recognize(rawToken);

                    if (optional.isPresent()) {
                        tokenType = optional.get();
                    } else if (type == UNKNOWN) {
                        tokenType = BasicToken.T_UNKNOWN;
                    }

                    yield entry(tokenType, rawToken, counter);
                }
                case NUMBER -> {
                    // Detect integer and floating-point numbers
                    Token.Type tokenType = BasicToken.T_INT;
                    String     value     = rawToken.value();

                    if (value.contains(".") || value.toLowerCase().contains("e")) {
                        tokenType = BasicToken.T_FLOAT;
                    }

                    yield entry(tokenType, rawToken, counter);
                }

                default -> entry(BasicToken.T_UNKNOWN, rawToken, counter);
            };

            source.entry(rawToken.offset(), rawToken.length(), token.type());

            tokens.add(token);

            counter++;
        }

        tokens.add(entry(BasicToken.T_EOL, eol, counter));

        LOGGER.info("StringSource '{}' tokens", tokens.size());

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
