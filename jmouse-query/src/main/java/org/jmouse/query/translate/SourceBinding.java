package org.jmouse.query.translate;

import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.TranslationRefusedException;

import org.jmouse.query.el.node.ViewNode;

import java.util.Collection;

/**
 * Resolving {@code view … on $source} — the rule, stated once.
 *
 * <h2>⚠️ Why this is not left to each backend</h2>
 *
 * <p>Every translator has to answer the same question — <em>which source does this view actually run
 * against?</em> — and the answer carries a safety property that is easy to lose. A backend implementing
 * it for itself would sooner or later be the one that took the caller's string and used it directly, and
 * nothing about that failure is visible: the query works, against the wrong data.</p>
 *
 * <h2>⚠️ A binding CHOOSES a source; it never creates one</h2>
 *
 * <p>The value bound to {@code $source} is a <strong>name</strong>, and it is refused unless the
 * application already declared a source under it. So the worst a caller can do with a value they control
 * is pick a different one of the sources somebody deliberately registered — which is an ordinary
 * decision, not an escape.</p>
 *
 * <p>That is the same rule a source declaration already lives under: a {@code .jmq} file may not name its
 * own tenant or workspace, because a file able to name its own scope is a file able to read somebody
 * else's data. A caller-supplied source name would have reopened exactly that door.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class SourceBinding {

    private SourceBinding() {
    }

    /**
     * Which source this view is about, resolving a late-bound target against what the caller supplied.
     *
     * @param view     the view, pinned ({@code on issues}) or late-bound ({@code on $source})
     * @param bindings what the caller supplied by name
     * @param declared the names the application registered, for the refusals
     * @return the source's name
     */
    public static String resolve(ViewNode view, Bindings bindings, Collection<String> declared) {
        if (!view.isTargetBound()) {
            return view.getTarget();
        }

        String binding = view.getTarget();

        if (!bindings.has(binding)) {
            throw new TranslationRefusedException(
                    ("this view runs against '$%s' and nothing was bound to it; "
                     + "supply it by name, choosing one of %s").formatted(binding, list(declared)));
        }

        Object value = bindings.value(binding);

        // ⚠️ A name, and only a name. Anything else — a source object, a table, a statement — would make
        // the caller the one deciding what gets read rather than which of the declared things gets read.
        if (!(value instanceof String name) || name.isBlank()) {
            throw new TranslationRefusedException(
                    ("'$%s' has to be bound to the NAME of a declared source; "
                     + "it was bound to %s").formatted(binding, describe(value)));
        }

        if (!declared.contains(name)) {
            throw new TranslationRefusedException(
                    ("'$%s' was bound to '%s', and nothing is declared under that name; "
                     + "this engine has %s").formatted(binding, name, list(declared)));
        }

        return name;
    }

    private static String list(Collection<String> declared) {
        return declared.isEmpty() ? "nothing at all" : String.join(", ", declared);
    }

    private static String describe(Object value) {
        return value == null ? "nothing" : "a " + value.getClass().getSimpleName();
    }
}
