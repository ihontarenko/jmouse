package org.jmouse.spice.expression;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.formula.Formula;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * SPICE's expressions — {@code {2*R1}}, {@code '1/(2*pi*R*C)'}, a {@code .param}'s right side — as a dialect of jME,
 * in both directions: read into a jMF {@link Formula} over the parameters, and a formula written back as ngspice reads
 * it in a value or a B source.
 *
 * <p>The dialect is assembled as jMF is: a jME container with SPICE's vocabulary ({@link SpiceCoreExtension}), its
 * operator and primary grammars substituted at jME's lookup ({@link SpiceExtensionContainer}), and jME's lexer in its
 * mathematical dialect. The tree jME builds is read into jMF's by {@link SpiceFormulaTranslator}; a formula is written
 * by walking it ({@link SpiceFormulaWriter}). Nothing is rewritten as text on the way.</p>
 *
 * <p>The standard instance is shared and thread-safe for reading, and caches the trees of the expressions it read.</p>
 */
public final class SpiceExpressionLanguage {

    private static final SpiceExpressionLanguage STANDARD = new SpiceExpressionLanguage();

    private final Lexer                   lexer;
    private final ParserContext           context;
    /** The trees of the expressions read, by their text: a deck says one value many times. */
    private final Map<String, Expression> trees = new ConcurrentHashMap<>();

    private SpiceExpressionLanguage() {
        this.lexer = new DefaultLexer(new DefaultTokenizer(
                new ExpressionSplitter(ExpressionSplitter.Dialect.MATHEMATICS), new ExpressionRecognizer()));
        this.context = new DefaultParserContext(new SpiceExtensionContainer());
    }

    public static SpiceExpressionLanguage standard() {
        return STANDARD;
    }

    /**
     * Reads a SPICE expression as a jMF formula.
     *
     * @param written    the expression as written, without its braces or quotes
     * @param position   where it was written — its opening delimiter —, for a refusal's line and column
     * @param parameters the parameters it may name, each by its lower-case name → the spelling it was declared with
     * @throws SpiceParseException when it does not read, names what is not declared, or uses what jMF cannot say
     */
    public Formula read(String written, SpicePosition position, Map<String, String> parameters) {
        if (written == null || written.isBlank()) {
            throw new SpiceParseException(position, "an expression is empty where a value is wanted");
        }
        Expression tree;
        try {
            tree = trees.computeIfAbsent(written, this::tree);
        } catch (SpiceExpressionRefusal refusal) {
            throw new SpiceParseException(at(position, refusal.offset()), refusal.getMessage());
        } catch (RuntimeException unreadable) {
            throw new SpiceParseException(position, "'" + written.strip() + "' does not read as an expression: "
                    + unreadable.getMessage());
        }
        try {
            return new SpiceFormulaTranslator(parameters).translate(tree);
        } catch (SpiceExpressionRefusal refusal) {
            throw new SpiceParseException(at(position, refusal.offset()), refusal.getMessage());
        }
    }

    /**
     * Writes a jMF formula as ngspice reads it.
     *
     * @param variables how each name the formula reads is written in SPICE — a parameter, {@code V(node)},
     *                  {@code time}; it throws {@link UntranslatableFormula} for a name SPICE cannot read
     * @throws UntranslatableFormula when the formula holds what SPICE cannot say
     */
    public String write(Formula formula, Function<String, String> variables) {
        return new SpiceFormulaWriter(variables).written(formula, 0);
    }

    /** The tree of one expression, every token of it read: one left over is refused where it stands. */
    private Expression tree(String written) {
        TokenCursor cursor = lexer.tokenize(new StringSource("SPICE EXPRESSION", written));
        cursor.consumeIf(BasicToken.T_SOL);
        Expression tree = (Expression) context.getParser(ExpressionParser.class).parse(cursor, context);
        cursor.consumeIf(BasicToken.T_EOL);
        if (cursor.hasNext()) {
            Token unread = cursor.peek();
            throw new SpiceExpressionRefusal(unread.offset(), leftOver(unread));
        }
        return tree;
    }

    /** Why a token was left unread: a value with no operator before it, or what SPICE's expressions do not have. */
    private static String leftOver(Token unread) {
        if (unread.type() == BasicToken.T_CLOSE_PAREN) {
            return "')' closes a parenthesis that was never opened";
        }
        if (unread.type() == BasicToken.T_IDENTIFIER || unread.type() == BasicToken.T_NUMERIC
                || unread.type() == BasicToken.T_OPEN_PAREN) {
            return "'" + unread.value() + "' follows a value with no operator between them";
        }
        return "'" + unread.value() + "' is not part of a SPICE expression";
    }

    /** A place in the expression's text on the deck's line: its columns follow the opening delimiter's. */
    private static SpicePosition at(SpicePosition position, int offset) {
        return offset < 0 ? position : new SpicePosition(position.line(), position.column() + 1 + offset);
    }
}
