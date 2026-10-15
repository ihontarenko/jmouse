package org.jmouse.formula.el;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * jME's lexer, with one repair a formula cannot live without: jME's splitter reads a sign glued to a number as
 * part of the literal, so {@code b+2.5} arrives as {@code b} followed by the literal {@code +2.5} and does not
 * parse. After an operand such a sign is an operator, and this lexer splits it back out. A sign at the start or
 * after an operator stays part of the number, which is exactly what jME already does.
 */
public class FormulaLexer implements Lexer {

    private static final Set<Token.Type> NUMBERS = Set.of(BasicToken.T_NUMERIC, BasicToken.T_INT, BasicToken.T_LONG,
            BasicToken.T_FLOAT, BasicToken.T_DOUBLE, BasicToken.T_BYTE, BasicToken.T_SHORT);
    private static final Set<Token.Type> OPERAND_ENDS = Set.of(BasicToken.T_IDENTIFIER, BasicToken.T_CLOSE_PAREN,
            BasicToken.T_NUMERIC, BasicToken.T_INT, BasicToken.T_LONG, BasicToken.T_FLOAT, BasicToken.T_DOUBLE,
            BasicToken.T_BYTE, BasicToken.T_SHORT);

    private final DefaultTokenizer tokenizer = new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer());

    @Override
    public TokenCursor tokenize(CharSequence text) {
        if (!(text instanceof TokenizableSource source)) {
            return null;
        }
        List<Token> original = withoutTypeSuffixes(tokenizer.tokenize(source), source);
        List<Token> repaired = new ArrayList<>(original.size() + 4);
        Token previous = null;
        for (Token token : original) {
            if (previous != null && NUMBERS.contains(token.type()) && OPERAND_ENDS.contains(previous.type())
                    && (token.value().startsWith("-") || token.value().startsWith("+"))) {
                boolean minus = token.value().startsWith("-");
                repaired.add(new Token(minus ? "-" : "+", minus ? BasicToken.T_MINUS : BasicToken.T_PLUS, 0,
                        token.offset(), token.lineNumber()));
                Token number = new Token(token.value().substring(1), token.type(), 0, token.offset() + 1, token.lineNumber());
                repaired.add(number);
                previous = number;
                continue;
            }
            repaired.add(token);
            previous = token;
        }
        List<Token> numbered = new ArrayList<>(repaired.size());
        for (int ordinal = 0; ordinal < repaired.size(); ordinal++) {
            Token token = repaired.get(ordinal);
            numbered.add(new Token(token.value(), token.type(), ordinal, token.offset(), token.lineNumber()));
        }
        return new Cursor(source, numbered);
    }

    /**
     * jME reads a letter glued to a number as a Java type suffix — {@code 2i} an int, {@code 3f} a float, {@code 2d} a
     * double — keeps the number and drops the letter. A formula has no type suffixes: {@code 0.156i} is 0.156 times the
     * imaginary unit and {@code 2d} two times {@code d}. So the letter comes back as the identifier it is, after the
     * number, and implicit multiplication reads the two as a product. Without this {@code 2i} quietly read as 2 and
     * {@code 0.156i} did not read at all.
     */
    private static List<Token> withoutTypeSuffixes(List<Token> tokens, CharSequence text) {
        List<Token> repaired = new ArrayList<>(tokens.size() + 2);
        for (Token token : tokens) {
            int suffix = token.offset() + token.value().length();
            boolean typed = NUMBERS.contains(token.type()) && token.type() != BasicToken.T_NUMERIC && token.offset() >= 0
                    && suffix < text.length() && Character.isLetter(text.charAt(suffix));
            if (!typed) {
                repaired.add(token);
                continue;
            }
            repaired.add(new Token(token.value(), BasicToken.T_NUMERIC, 0, token.offset(), token.lineNumber()));
            repaired.add(new Token(String.valueOf(text.charAt(suffix)), BasicToken.T_IDENTIFIER, 0, suffix, token.lineNumber()));
        }
        return repaired;
    }
}
