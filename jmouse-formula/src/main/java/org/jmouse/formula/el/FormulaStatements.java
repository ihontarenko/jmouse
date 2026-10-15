package org.jmouse.formula.el;

import org.jmouse.formula.FormulaException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a formula document into its statements before jME sees any of them. A document is definitions, then
 * one expression:
 *
 * <pre>
 * # a comment runs to the end of its line
 * r = sqrt(x^2 + y^2)
 * f(u, v) = exp(-u^2) cos(v)
 * f(r, 3 r)
 * </pre>
 *
 * <p>A statement ends at a {@code ;}, or at a line break once it is a whole definition — so an expression may
 * run over several lines, and so may a definition inside parentheses or after a line ending in an operator. A single {@code =} at the top level of the
 * last statement makes it an equation; {@code ==} is a comparison everywhere.
 */
final class FormulaStatements {

    private static final String NAME = "[A-Za-z\\p{IsGreek}][A-Za-z0-9_\\p{IsGreek}]*";
    private static final Pattern DEFINITION = Pattern.compile(
            "\\s*(" + NAME + ")\\s*(?:\\(\\s*(" + NAME + "(?:\\s*,\\s*" + NAME + ")*)?\\s*\\))?\\s*=(?!=)(.*)",
            Pattern.DOTALL);

    record Definition(String name, List<String> parameters, String body) {
    }

    record Document(List<Definition> definitions, String expression) {
    }

    private FormulaStatements() {
    }

    static Document split(String source) {
        List<String> statements = statements(withoutComments(source));
        if (statements.isEmpty()) {
            throw new FormulaException("A formula is required.", source, -1);
        }
        List<Definition> definitions = new ArrayList<>();
        for (int index = 0; index < statements.size() - 1; index++) {
            String statement = statements.get(index);
            definitions.add(definition(statement).orElseThrow(() -> new FormulaException("'" + statement.strip()
                    + "' is neither a definition nor the last statement; only the last one is the formula itself.",
                    source, source.indexOf(statement.strip()))));
        }
        return new Document(definitions, statements.getLast());
    }

    /** {@code f(x, y) = body} or {@code r = body}, or nothing when the statement is not shaped like one. */
    static Optional<Definition> definition(String statement) {
        Matcher matcher = DEFINITION.matcher(statement);
        if (!matcher.matches() || matcher.group(3).isBlank()) {
            return Optional.empty();
        }
        List<String> parameters = matcher.group(2) == null ? List.of()
                : Arrays.stream(matcher.group(2).split(",")).map(String::strip).toList();
        return Optional.of(new Definition(matcher.group(1), parameters, matcher.group(3).strip()));
    }

    /** The two sides of a top-level single {@code =}, or nothing when there is none. */
    static Optional<String[]> equationSides(String expression) {
        int depth = 0;
        int found = -1;
        for (int index = 0; index < expression.length(); index++) {
            char character = expression.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
            } else if (character == '=' && depth == 0) {
                char before = index > 0 ? expression.charAt(index - 1) : ' ';
                char after = index + 1 < expression.length() ? expression.charAt(index + 1) : ' ';
                if (after == '=') {
                    index++;
                    continue;
                }
                if (before == '<' || before == '>' || before == '!' || before == '=') {
                    continue;
                }
                if (found >= 0) {
                    throw new FormulaException("An equation has one '='; compare with '==' instead.", expression, index);
                }
                found = index;
            }
        }
        if (found < 0) {
            return Optional.empty();
        }
        return Optional.of(new String[]{expression.substring(0, found), expression.substring(found + 1)});
    }

    private static String withoutComments(String source) {
        StringBuilder kept = new StringBuilder(source.length());
        boolean comment = false;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '#') {
                comment = true;
            } else if (character == '\n' || character == '\r') {
                comment = false;
            }
            kept.append(comment ? ' ' : character);
        }
        return kept.toString();
    }

    private static List<String> statements(String source) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
            }
            boolean lineBreak = character == '\n' || character == '\r';
            boolean ends = depth == 0 && (character == ';'
                    || (lineBreak && isComplete(current) && definition(current.toString()).isPresent()));
            if (ends) {
                add(statements, current);
                continue;
            }
            current.append(lineBreak ? ' ' : character);
        }
        add(statements, current);
        return statements;
    }

    /** A definition whose line ends in an operator or a comma goes on to the next line. */
    private static boolean isComplete(StringBuilder current) {
        String text = current.toString().strip();
        return !text.isEmpty() && "+-*/^,=<>&|!".indexOf(text.charAt(text.length() - 1)) < 0;
    }

    private static void add(List<String> statements, StringBuilder current) {
        if (!current.toString().isBlank()) {
            statements.add(current.toString().strip());
        }
        current.setLength(0);
    }
}
