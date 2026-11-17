package org.jmouse.el.lexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * What a language's numbers, operators and texts look like, where languages built on the engine differ: the
     * {@link Feature}s it has.
     *
     * <p>⚠️ A number never carries a sign. Whether {@code -} negates or subtracts is decided by what stands
     * before it — {@code b-2.5} subtracts, {@code -2.5} negates — and only the parser knows that; a lexer that
     * glued the sign on read {@code b-2.5} as {@code b} followed by the number {@code -2.5}.</p>
     *
     * @param features what this dialect has; one it lacks reads as that feature says it reads without it
     */
    public record Dialect(Set<Feature> features) {

        /** jME's own: typed numbers and step operators, as Java writes them. */
        public static final Dialect PROGRAMMING = of(Feature.TYPED_NUMBERS, Feature.STEP_OPERATORS);

        /** A formula's: no type suffixes and no steps. */
        public static final Dialect MATHEMATICS = of();

        public Dialect {
            EnumSet<Feature> copied = EnumSet.noneOf(Feature.class);
            copied.addAll(features);
            features = Collections.unmodifiableSet(copied);
        }

        /** The dialect of these features. */
        public static Dialect of(Feature... features) {
            return new Dialect(Set.of(features));
        }

        /** This dialect, with {@code feature} too. */
        public Dialect with(Feature feature) {
            EnumSet<Feature> more = EnumSet.of(feature);
            more.addAll(features);
            return new Dialect(more);
        }

        public boolean has(Feature feature) {
            return features.contains(feature);
        }
    }

    /** What a {@link Dialect} may have. */
    public enum Feature {

        /**
         * A letter ending a number is a Java type suffix — {@code 3d} a double, {@code 2L} a long. Only a letter that ends the
         * word is one: {@code 24c02} is a number and a name. Without it — mathematics — {@code 2d} is two times {@code d}.
         */
        TYPED_NUMBERS,

        /** {@code ++} and {@code --} are operators of their own. Without it — mathematics — {@code 2--1} is two minus minus one. */
        STEP_OPERATORS,

        /**
         * {@code """ … """} is one string, line breaks and quotes inside it included — a document's long text. A long text left
         * open runs to the end of the source. Without it, {@code """} is an empty string followed by a quote.
         */
        LONG_TEXTS,

        /**
         * {@code #} opens a comment that runs to the end of its line and is passed over as space is — no token stands for it.
         * Without it, {@code #} is a character like any other.
         */
        LINE_COMMENTS
    }

    private final Pattern expressionPattern;
    private final Dialect dialect;

    public ExpressionSplitter() {
        this(Dialect.PROGRAMMING);
    }

    public ExpressionSplitter(Dialect dialect) {
        this.expressionPattern = expressionPattern(dialect);
        this.dialect = dialect;
    }

    private static Pattern expressionPattern(Dialect dialect) {
        String typeSuffix = dialect.has(Feature.TYPED_NUMBERS) ? "(?:[FLIDSBClfidsbc](?!" + Identifiers.CHARACTER + "))?" : "";
        String steps = dialect.has(Feature.STEP_OPERATORS) ? "\\+\\+|--|" : "";
        // Tried before a quoted string, which would otherwise read its first two quotes as an empty string.
        String longText = dialect.has(Feature.LONG_TEXTS) ? "\"\"\"[\\s\\S]*?(?:\"\"\"|\\z)|" : "";
        String comment = dialect.has(Feature.LINE_COMMENTS) ? "|(?<COMMENT>#[^\\n\\r]*)" : "";
        return Pattern.compile(
                "[^\\S\\r\\n]*(?:(?<IDENTIFIER>" + Identifiers.PATTERN + ")" +
                "|(?<NUMBER>(?<!\\d)(\\d+(\\.\\d+)?([Ee][+-]?\\d+)?" + typeSuffix + "))" +
                // ⚠️ The character classes below must NOT exclude '|'. They used to read [^'|\n\r], which
                // looks like alternation and is not: inside a character class the pipe is a literal, so the
                // effect was to forbid the one character quoting exists to protect. `'a|b'` failed to parse
                // anywhere in the language — and a value like "3300|mΩ" is ordinary data, not an edge case.
                "|(?<STRING>" + longText + "'[^'\\n\\r]*'|\"[^\"\\n\\r]*\")" +
                comment +
                "|(?<OPERATOR>\\?\\?|->|\\.\\.|<=|>=|!=|==|&&|\\|\\||-=|\\+=|" + steps + "\\*\\*|[-+*/%^><=!])" +
                "|(?<NL>[\\n\\r]+)" +
                "|(?<OTHER>\\S))"
        );
    }

    public static final String IDENTIFIER = "IDENTIFIER";
    public static final String NUMBER     = "NUMBER";
    public static final String STRING     = "STRING";
    public static final String NEW_LINE   = "NL";
    public static final String OPERATOR   = "OPERATOR";
    public static final String OTHER      = "OTHER";
    /** A {@code #} comment, in a dialect that has them: matched so it can be passed over, never a token. */
    public static final String COMMENT    = "COMMENT";

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
        Matcher      matcher = expressionPattern.matcher(segment);
        int          index   = 0;

        while (matcher.find(index)) {
            // A comment is passed over as space is: it ends where its line does, and nothing stands for it.
            if (dialect.has(Feature.LINE_COMMENTS) && matcher.group(COMMENT) != null) {
                index = matcher.end() > index ? matcher.end() : index + 1;
                continue;
            }

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
