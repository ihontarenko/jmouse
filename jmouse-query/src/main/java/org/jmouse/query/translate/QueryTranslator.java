package org.jmouse.query.translate;

import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Translator;
import org.jmouse.query.el.node.ClauseKind;
import org.jmouse.query.el.node.ClauseNode;
import org.jmouse.query.el.node.QueryBlockNode;

/**
 * A {@link Translator} that knows it is translating a query. 🧭
 *
 * <p>The seam itself is shared — three languages on this expression engine translate their trees the
 * same way, and {@link Translator} is that shape with nothing of any one of them in it. What is left
 * here is the one convenience that could not be shared, because it names this language's own nodes.</p>
 *
 * @param <T> what translating produces for this destination
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface QueryTranslator<T> extends Translator<T> {

    /**
     * Refuses every clause this translator cannot honour, before translating any of them.
     *
     * <h2>⚠️ Up front, and never silently</h2>
     *
     * <p>Checked before anything is built, so a document asking for two things this destination lacks is
     * told about the first rather than handed half a statement. And <strong>refused</strong> rather than
     * ignored: quietly dropping a {@code group} and returning ungrouped rows is the bug this whole area
     * exists to make impossible to write by accident.</p>
     *
     * <p>⚠️ It asks each clause what it needs rather than knowing the clauses itself, so a clause added
     * to the language costs no edit here and none in any translator.</p>
     *
     * @param block what was asked for
     */
    default void requireSupport(QueryBlockNode block) {
        Capabilities capabilities = capabilities();

        for (ClauseNode clause : block.getClauses()) {
            ClauseKind kind = clause.kind();

            capabilities.require(kind.capability(), kind.keyword());
        }
    }
}
