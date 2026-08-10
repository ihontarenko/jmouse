package org.jmouse.access.el.node;

import org.jmouse.access.el.PolicyParseException;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.stream.Collectors;

/**
 * A {@code { … }} block, and the two things every one of them owes its contents.
 *
 * <p><strong>Nothing is dropped silently.</strong> Blocks used to keep the children they recognised
 * and ignore the rest, which turned a statement written in the wrong block into a rule that simply
 * was not there — in a file whose whole job is to say who may do what, the worst possible failure.
 * {@link #reject(Expression, String)} is how a block says no instead.</p>
 *
 * <p><strong>It can be written back out.</strong> Not for machines: the control room has to show an
 * administrator the file that explains what they are looking at.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class PolicyBlockNode extends ExpressionsNode {

    private static final String INDENT     = "    ";
    private static final String LINE_BREAK = "\n";

    /**
     * Refuses a statement this block cannot hold, naming where it was written.
     *
     * @param expression the offending child
     * @param expected   what this block does hold, phrased for whoever is editing the file
     * @return never — the return type only lets callers write {@code throw reject(…)}
     * @throws PolicyParseException always
     */
    protected PolicyParseException reject(Expression expression, String expected) {
        throw new PolicyParseException(
                SourceSpanNode.at(expression),
                "%s cannot hold this statement; %s".formatted(describe(), expected)
        );
    }

    /**
     * Names this block the way it is written in a file, for use in a failure message.
     *
     * @return the block's keyword, such as {@code role} or {@code subject}
     */
    protected abstract String describe();

    /**
     * Renders this block and everything in it.
     *
     * <p>The line break is {@code \n} rather than the platform's, because what this produces is a
     * {@code .jmp} file's text — the same on every machine that reads it back.</p>
     *
     * @param header the text before the opening brace, such as {@code role SPACE_ADMIN}
     * @return the block as source
     */
    protected String renderBlock(String header) {
        return header + " {" + LINE_BREAK + renderExpressions(INDENT.length()) + "}" + LINE_BREAK;
    }

    /**
     * Renders everything in this block, one declaration per line.
     *
     * @param indent how far in to move each line
     * @return the block's contents as source
     */
    protected String renderExpressions(int indent) {
        return getExpressions().stream()
                .map(Expression::toSource)
                .map(source -> source.indent(indent))
                .collect(Collectors.joining());
    }
}
