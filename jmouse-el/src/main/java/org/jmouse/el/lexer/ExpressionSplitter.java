package org.jmouse.el.lexer;

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
            "[^\\S\\r\\n]*(?:(?<IDENTIFIER>[a-zA-Z_][a-zA-Z0-9_]*)" +
            "|(?<NUMBER>(?<!\\d)([+-]?\\d+(\\.\\d+)?([Ee][+-]?\\d+)?[FLIDSBClfidsbc]?))" +
            // ⚠️ The character classes below must NOT exclude '|'. They used to read [^'|\n\r], which
            // looks like alternation and is not: inside a character class the pipe is a literal, so the
            // effect was to forbid the one character quoting exists to protect. `'a|b'` failed to parse
            // anywhere in the language — and a value like "3300|mΩ" is ordinary data, not an edge case.
            "|(?<STRING>'[^'\\n\\r]*'|\"[^\"\\n\\r]*\")" +
            "|(?<OPERATOR>\\?\\?|->|\\.\\.|<=|>=|!=|==|&&|\\|\\||-=|\\+=|\\+\\+|--|\\*\\*|[-+*/%^><=!])" +
            "|(?<NL>[\\n\\r]+)" +
            "|(?<OTHER>\\S))"
    );

    public static final String IDENTIFIER = "IDENTIFIER";
    public static final String NUMBER     = "NUMBER";
    public static final String STRING     = "STRING";
    public static final String NEW_LINE   = "NL";
    public static final String OPERATOR   = "OPERATOR";
    public static final String OTHER      = "OTHER";

    // Define group names and their corresponding type types.
    private static final String[]                   GROUP_NAMES         = {IDENTIFIER, NUMBER, STRING, NEW_LINE, OPERATOR, OTHER};
    private static final Map<String, RawToken.Type> GROUP_TO_TOKEN_TYPE = new HashMap<>();

    static {
        GROUP_TO_TOKEN_TYPE.put(IDENTIFIER, RawToken.Type.IDENTIFIER);
        GROUP_TO_TOKEN_TYPE.put(NUMBER, RawToken.Type.NUMBER);
        GROUP_TO_TOKEN_TYPE.put(STRING, RawToken.Type.STRING);
        GROUP_TO_TOKEN_TYPE.put(NEW_LINE, RawToken.Type.NEW_LINE);
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
                    LOGGER.trace("Found group '{}' in tag '{}'", tokenType, tokenValue);
                    break;
                }
            }

            if (tokenValue != null) {
                tokens.add(new RawToken(tokenValue, text.getLineNumber(startOffset), startOffset, tokenType));
            }

            // protection of infinite loop
            int end = matcher.end();

            if (end == index) {
                index++;
            } else {
                index = end;
            }
        }

        // ⚠️ TRACE, and it has to be. Splitting happens once per lex, a lex happens inside anything
        // that compiles an expression, and one of those callers is the authorization path — so at
        // INFO this line is written for every request, quoting the whole source it just read.
        LOGGER.trace("Segment '{}' at offset '{}' and length '{}' splitted to: {} tokens",
                     segment, offset, length, tokens.size());

        return tokens;
    }
}
