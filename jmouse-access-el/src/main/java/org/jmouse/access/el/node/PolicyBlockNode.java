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
        return prefixOf(header) + header + " {" + LINE_BREAK
               + renderExpressions(INDENT.length()) + "}" + LINE_BREAK;
    }

    /**
     * Which of {@code declare} / {@code assign} this block is written with.
     *
     * <p>⚠️ <strong>The grammar accepts both spellings; this writes only one.</strong> Stored policy
     * revisions are source text and the control room can revert to any of them, so refusing the bare
     * form would make every revision written before the prefix existed unloadable — turning revert
     * into a way to break an installation. Accepting both and writing one means a document converges
     * on the canonical form the first time it is saved through the editor: no migration, no backfill.
     *
     * <p>The split is ADR-0018's line and nothing else — <em>the document owns structure, the row owns
     * the case</em>. A block saying what exists at all is {@code declare}; a block saying who has what
     * is {@code assign}.
     *
     * <p>⚠️ Read off the header's first word rather than declared per subclass. Nine one-word overrides
     * are nine places for the tenth block to be forgotten in, and the header is written by the very
     * node being asked — so there is no second source of truth for it to disagree with.
     */
    private static String prefixOf(String header) {
        return switch (header.split(" ", 2)[0]) {
            case "subject", "entitlements" -> "assign ";
            case "scopes", "permissions", "actions", "capabilities", "plans", "role" -> "declare ";
            // `policy` wraps a document rather than stating anything in it, and `plan` is a line
            // inside `declare plans` rather than a block of its own. Neither takes a prefix.
            default -> "";
        };
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
