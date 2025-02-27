package org.jmouse.template.lexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits raw text into tokens, distinguishing between plain text and templating expressions.
 *
 * <p>Uses a regex-based approach to identify expressions enclosed within template delimiters,
 * such as <code>{{expression}}</code> or <code>{% statement %}</code>, and processes them accordingly.</p>
 *
 * <pre>{@code
 * RawSplitter splitter = new RawSplitter();
 * List<RawToken> tokens = splitter.split("Hello {{name}}!", 0, "Hello {{name}}!".length());
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RawSplitter implements Splitter<List<RawToken>> {

    private final static Logger LOGGER = LoggerFactory.getLogger(RawSplitter.class);

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "(?<OPEN>\\{(?<type>\\{|[%!#\\$\\*@])\\s*)" +  // Open: matches either "{{" or "{X" where X ∈ {%, !, #, $, *, @}
            "(?<INNER>.*?)\\s*" +                          // Inner: non-greedy match for the expression content
            "(?<CLOSE>(?:\\}\\}|\\k<type>\\}))"            // Close: matches "}}" if type was "{" or "X}" if type is X
    );
    public static final String INNER_GROUP = "INNER";
    public static final String CLOSE_GROUP = "CLOSE";
    public static final String OPEN_GROUP  = "OPEN";

    private final Splitter<List<RawToken>> splitter;

    public RawSplitter() {
        this.splitter = new ExpressionSplitter();
    }

    /**
     * Splits the given text into raw tokens, distinguishing between plain text and template expressions.
     *
     * @param text   the input character sequence
     * @param offset the starting offset for tokenization
     * @param length the number of characters to process
     * @return a list of {@link RawToken} representing the parsed components
     */
    @Override
    public List<RawToken> split(CharSequence text, int offset, int length) {
        List<RawToken> tokens = new ArrayList<>();

        // Create a subregion for tokenization.
        CharSequence subText   = text.subSequence(offset, offset + length);
        Matcher      matcher   = EXPRESSION_PATTERN.matcher(subText);
        int          lastIndex = 0;

        while (matcher.find()) {
            int matchStart = matcher.start();

            // Add HTML text (non-expression) as a single raw token.
            if (matchStart > lastIndex) {
                String htmlText = subText.subSequence(lastIndex, matchStart).toString();
                tokens.add(new RawToken(htmlText, offset + lastIndex, RawToken.Type.RAW_TEXT));
                LOGGER.info("Raw token: '{}'", htmlText);
            }

            // Process expression block if present.
            if (matcher.group(INNER_GROUP) != null) {
                String open  = matcher.group(OPEN_GROUP);
                String inner = matcher.group(INNER_GROUP);
                String close = matcher.group(CLOSE_GROUP);

                // Add the opening delimiter.
                tokens.add(new RawToken(open, offset + matcher.start(), RawToken.Type.OPEN_TAG));
                LOGGER.info("Open tag: {}", open);

                // Tokenize the inner content using ExpressionSplitter.
                int innerOffset = offset + matcher.start() + open.length();
                LOGGER.info("Inner expression: '{}'", inner);
                tokens.addAll(splitter.split(inner, innerOffset, inner.length()));

                // Add the closing delimiter.
                int closeTagPosition = offset + matcher.end() - close.length();
                tokens.add(new RawToken(close, closeTagPosition, RawToken.Type.CLOSE_TAG));
                LOGGER.info("Close tag: {}", close);
            } else {
                // Fallback: add the match as a token.
                String token = matcher.group();
                tokens.add(new RawToken(token, offset + matcher.start(), RawToken.Type.UNKNOWN));
                LOGGER.info("Fallback: {}", token);
            }

            lastIndex = matcher.end();
        }

        // Add remaining HTML text.
        if (lastIndex < subText.length()) {
            String tail = subText.subSequence(lastIndex, subText.length()).toString();
            tokens.add(new RawToken(tail, offset + lastIndex, RawToken.Type.RAW_TEXT));
            LOGGER.info("Tail: '{}'", tail);
        }

        return tokens;
    }
}
