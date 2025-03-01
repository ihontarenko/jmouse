package org.jmouse.template.lexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jmouse.template.lexer.RawToken.Type.*;

/**
 * Splits raw text into tokens, distinguishing between plain text and templating expressions.
 *
 * <p>Uses a regex-based approach to identify expressions enclosed within template delimiters,
 * such as <code>{{expression}}</code> or <code>{% statement %}</code>, and processes them accordingly.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RawSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    private final static Logger LOGGER      = LoggerFactory.getLogger(RawSplitter.class);
    public static final  String INNER_GROUP = "INNER";
    public static final  String CLOSE_GROUP = "CLOSE";
    public static final  String OPEN_GROUP  = "OPEN";

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "(?<OPEN>\\{(?<type>\\{|[%!#\\$\\*@])\\s*)" +  // Open: matches either "{{" or "{X" where X ∈ {%, !, #, $, *, @}
            "(?<INNER>.*?)\\s*" +                          // Inner: non-greedy match for the expression content
            "(?<CLOSE>(?:\\}\\}|\\k<type>\\}))"            // Close: matches "}}" if type was "{" or "X}" if type is X
    );

    private final Splitter<List<RawToken>, TokenizableSource> splitter;

    public RawSplitter() {
        this.splitter = new ExpressionSplitter();
    }

    /**
     * Splits the given source into raw tokens, distinguishing between plain source and template expressions.
     *
     * @param source the input character sequence
     * @param offset the starting offset for tokenization
     * @param length the number of characters to process
     * @return a list of {@link RawToken} representing the parsed components
     */
    @Override
    public List<RawToken> split(TokenizableSource source, int offset, int length) {
        List<RawToken> tokens = new ArrayList<>();

        // Create a subregion for tokenization.
        CharSequence segment   = source.subSequence(offset, length);
        Matcher      matcher   = EXPRESSION_PATTERN.matcher(segment);
        int          lastIndex = 0;

        while (matcher.find()) {
            int startIndex  = matcher.start();
            int startOffset = offset + startIndex;

            // Extract plain text between expressions.
            if (startIndex > lastIndex) {
                String rawContent = segment.subSequence(lastIndex, startIndex).toString();
                tokens.add(new RawToken(rawContent, source.getLineNumber(offset + lastIndex), offset + lastIndex, RAW_TEXT));
                LOGGER.info("Before open-close: '{}'", rawContent);
            }

            if (matcher.group(INNER_GROUP) != null) {
                String open       = matcher.group(OPEN_GROUP);
                String expression = matcher.group(INNER_GROUP);
                String close      = matcher.group(CLOSE_GROUP);

                // Add the opening delimiter.
                LOGGER.info("Open: {}", open);
                tokens.add(new RawToken(open, source.getLineNumber(startOffset), startOffset, OPEN_TAG));

                // Tokenize the expression content using ExpressionSplitter.
                LOGGER.info("Inner Expression: '{}'", expression);
                int innerIndex  = startIndex + open.length();
                int innerOffset = offset + innerIndex;
                tokens.addAll(splitter.split(source, innerOffset, innerOffset + expression.length()));

                // Add the closing delimiter.
                LOGGER.info("Close: {}", close);
                int closeIndex  = matcher.end() - close.length();
                int closeOffset = offset + closeIndex;
                tokens.add(new RawToken(close, source.getLineNumber(closeOffset), closeOffset, CLOSE_TAG));
            }

            lastIndex = matcher.end();
        }

        // Add remaining HTML source.
        if (lastIndex < segment.length()) {
            String tail       = segment.subSequence(lastIndex, segment.length()).toString();
            int    lastOffset = offset + lastIndex;
            tokens.add(new RawToken(tail, source.getLineNumber(lastOffset), lastOffset, RAW_TEXT));
            LOGGER.info("Tail: '{}'", tail);
        }

        return tokens;
    }
}
