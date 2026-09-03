package org.jmouse.validator.el.translate;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Translator;
import org.jmouse.validator.el.node.CheckBlockNode;
import org.jmouse.validator.el.node.CheckLineNode;
import org.jmouse.validator.el.node.CheckNode;
import org.jmouse.validator.el.node.InvariantNode;
import org.jmouse.validator.el.node.ValidationDocumentNode;
import org.jmouse.validator.el.node.WhenBranchNode;
import org.jmouse.validator.el.node.WhenNode;

import java.util.List;

/**
 * A {@link Translator} that knows it is translating a validation document. 🧭
 *
 * <p>The seam itself is shared — several languages on this expression engine translate their trees the
 * same way, and {@link Translator} is that shape with nothing of any one of them in it. What is left
 * here is the one convenience that could not be shared, because it names this language's own nodes.</p>
 *
 * @param <T> what translating produces for this destination
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ValidationTranslator<T> extends Translator<T> {

    /**
     * Refuses every construct this translator cannot honour, before translating any of them.
     *
     * <h2>⚠️ Up front, and never silently</h2>
     *
     * <p>Checked before anything is built, so a document asking for two things this destination lacks
     * is told about the first rather than handed half a validation. And <strong>refused</strong> rather
     * than ignored: quietly dropping an {@code invariant} and returning a validation that passes
     * records it should have refused is the bug this whole area exists to make impossible to write by
     * accident.</p>
     *
     * @param node the document, or any part of one
     */
    default void requireSupport(Node node) {
        Capabilities capabilities = capabilities();

        switch (node) {
            // ⚠️ A whole document has to be handled here, not only its statements. Both call sites hand
            // this method the document itself, and falling through to `default` would check nothing at
            // all — a translator declaring less than the language, silently honouring more.
            case ValidationDocumentNode document -> requireSupport(document.getExpressions());
            case CheckBlockNode block -> {
                if (block.getKind() == CheckBlockNode.Kind.GATE) {
                    capabilities.require(JmvCapability.GATE, "gate");
                }

                requireSupport(block.getExpressions());
            }
            case WhenNode guard -> {
                capabilities.require(JmvCapability.GUARD, "when");

                for (WhenBranchNode branch : guard.getBranches()) {
                    requireSupport(branch.getExpressions());
                }
            }
            case InvariantNode ignored -> capabilities.require(JmvCapability.INVARIANT, "invariant");
            case CheckLineNode line -> requireChecks(line);
            default -> {
                // A node this language does not own carries no capability of its own to require.
            }
        }
    }

    /**
     * Refuses every construct a list of statements asks for and this translator lacks.
     *
     * @param nodes the statements
     */
    default void requireSupport(List<Expression> nodes) {
        for (Expression node : nodes) {
            requireSupport(node);
        }
    }

    /**
     * Refuses what one check line asks for beyond the checks themselves.
     *
     * @param line the line
     */
    private void requireChecks(CheckLineNode line) {
        Capabilities capabilities = capabilities();

        if (line.getMessage() != null) {
            capabilities.require(JmvCapability.MESSAGE, "a line message");
        }

        for (CheckNode check : line.getChecks()) {
            if (check.isStop()) {
                capabilities.require(JmvCapability.STOP, "stop");
            }

            if (check.getMessage() != null) {
                capabilities.require(JmvCapability.MESSAGE, "a check message");
            }
        }
    }
}
