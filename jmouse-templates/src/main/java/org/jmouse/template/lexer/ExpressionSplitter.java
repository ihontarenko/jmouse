package org.jmouse.template.lexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits expressions into individual tokens such as identifiers, numbers, strings, and operators.
 *
 * <p>Utilizes regex-based tokenization to extract meaningful components from an tag.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ExpressionSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExpressionSplitter.class);

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "\\s*(?:(?<IDENTIFIER>[a-zA-Z_][a-zA-Z0-9_]*)" +
            "|(?<NUMBER>(?<!\\d)([+-]?\\d+(\\.\\d+)?([Ee][+-]?\\d+)?))" +
            "|(?<STRING>'[^']*'|\"[^\"]*\")" +
            "|(?<OPERATOR><=|>=|!=|==|&&|\\|\\||-=|\\+=|\\+\\+|--|[-+*/%^><=!])" +
            "|(?<OTHER>\\S))"
    );

    public static final String IDENTIFIER = "IDENTIFIER";
    public static final String NUMBER     = "NUMBER";
    public static final String STRING     = "STRING";
    public static final String OPERATOR   = "OPERATOR";
    public static final String OTHER      = "OTHER";

    // Define group names and their corresponding type types.
    private static final String[]                   GROUP_NAMES         = {IDENTIFIER, NUMBER, STRING, OPERATOR, OTHER};
    private static final Map<String, RawToken.Type> GROUP_TO_TOKEN_TYPE = new HashMap<>();

    static {
        GROUP_TO_TOKEN_TYPE.put(IDENTIFIER, RawToken.Type.IDENTIFIER);
        GROUP_TO_TOKEN_TYPE.put(NUMBER, RawToken.Type.NUMBER);
        GROUP_TO_TOKEN_TYPE.put(STRING, RawToken.Type.STRING);
        GROUP_TO_TOKEN_TYPE.put(OPERATOR, RawToken.Type.OPERATOR);
        GROUP_TO_TOKEN_TYPE.put(OTHER, RawToken.Type.UNKNOWN);
    }

    /**
     * Splits the provided text into a list of {@link RawToken}, identifying different type types.
     *
     * @param text   the input character sequence
     * @param offset the starting offset for tokenization
     * @param length the number of characters to process
     * @return a list of {@link RawToken} extracted from the input text
     */
    @Override
    public List<RawToken> split(TokenizableSource text, int offset, int length) {
        List<RawToken> tokens = new ArrayList<>();

        // Create a sub-sequence of the text for processing.
        CharSequence segment = text.subSequence(offset, length);
        Matcher      matcher = EXPRESSION_PATTERN.matcher(segment);
        int          index   = 0;

        while (matcher.find(index)) {
            String        tokenValue  = null;
            RawToken.Type tokenType   = GROUP_TO_TOKEN_TYPE.get(OTHER);
            int           startOffset = offset + matcher.start();

            // Loop through the predefined group names to see which one matched.
            for (String groupName : GROUP_NAMES) {
                tokenValue = matcher.group(groupName);
                if (tokenValue != null) {
                    // Get the start position of the capturing group.
                    startOffset = offset + matcher.start(groupName);
                    tokenType = GROUP_TO_TOKEN_TYPE.get(groupName);
                    LOGGER.info("Found group '{}' in tag '{}'", tokenType, tokenValue);
                    break;
                }
            }

            if (tokenValue != null) {
                tokens.add(new RawToken(tokenValue, text.getLineNumber(startOffset), startOffset, tokenType));
            }

            index = matcher.end();
        }

        return tokens;
    }
}
