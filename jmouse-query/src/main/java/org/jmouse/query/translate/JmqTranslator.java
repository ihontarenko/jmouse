package org.jmouse.query.translate;

import org.jmouse.el.translate.Translator;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.TranslationRefusedException;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;

/**
 * Back into jMQ — the language rendering itself, on the same seam as every other destination.
 *
 * <h2>⚠️ Why this is a translator and not a method on a node</h2>
 *
 * <p>Rendering a tree as jMQ and compiling it into SQL are the same operation with a different
 * destination. While one of them was a method on the node and the other lived in a compiler, they were
 * two bodies of code walking one tree — and two walkers of one tree agree until the day somebody teaches
 * only one of them about a new node. Here, {@code translate(view, …)} into jMQ and
 * {@code translate(view, …)} into SQL differ by which object was constructed and by nothing else.</p>
 *
 * <h2>⚠️ Bindings are deliberately ignored</h2>
 *
 * <p>A translator into a backend substitutes nothing and <em>binds</em> its values; a translator back
 * into the language must not even do that. A saved view rendered with somebody's tenant baked into it is
 * a saved view that reads correctly and is wrong for everybody else — and it would be written to a
 * database by a builder that had no way of knowing.</p>
 *
 * <p>So what comes out is the query as it was written: names still names, canonical clause order, one
 * clause per line. Rendering an already-rendered document changes nothing, which is what makes the
 * output safe to diff and safe to store.</p>
 *
 * <p>⚠️ Comments do not survive — they are not part of what the document declares and the parser does not
 * keep them. Anything that rewrites a document a person may have commented has to say so.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmqTranslator implements Translator<String> {

    /**
     * ⚠️ Everything the language defines, and honestly so: this destination <em>is</em> the language, so
     * there is no clause it can be handed that it cannot write back out. It is the one translator for
     * which {@link Capabilities#everything} is a statement of fact rather than a claim about a backend.
     */
    private static final Capabilities CAPABILITIES = Capabilities.everything("jmq");

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    /**
     * ⚠️ {@code toSource()} lives on {@link Expression}, not on every {@link Node}, and the refusal here
     * is the useful half: a node kind that never learned to write itself back out would otherwise reach a
     * saved query as an empty string, and an empty clause is a query that means something else.
     */
    @Override
    public String translate(Node node, Bindings bindings) {
        if (node instanceof Expression expression) {
            return expression.toSource();
        }

        throw new TranslationRefusedException(
                "a %s cannot be written back out as jMQ; only an expression knows its own source"
                        .formatted(node.getClass().getSimpleName()));
    }
}
