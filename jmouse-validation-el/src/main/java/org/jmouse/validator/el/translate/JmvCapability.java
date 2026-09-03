package org.jmouse.validator.el.translate;

import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.Capability;

/**
 * What a destination for a {@code .jmv} tree may or may not be able to honour.
 *
 * <h2>⚠️ Every one of them is qualified, and that is the naming rule rather than caution</h2>
 *
 * <p>{@link Capability} reserves unqualified names for the language that defines them, and the
 * unqualified set already belongs to the query language — {@code filter}, {@code sort}, {@code join}.
 * A validation capability called {@code gate} would be minting a name in somebody else's space, so
 * every one here carries {@code validation.}.</p>
 *
 * <h2>⚠️ There are capabilities at all because destinations will differ</h2>
 *
 * <p>The runtime honours everything. A translator rendering a document into a form a browser can
 * evaluate will not honour {@code validation.invariant} on its own — a cross-field assertion needs
 * every field's value, which a per-field widget does not have. It must <strong>refuse</strong> the
 * document rather than render one that checks less than its author wrote.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmvCapability {

    /** {@code gate { … }} — a block whose failure answers for the whole document. */
    public static final Capability GATE = Capability.named("validation", "gate");

    /** {@code when … otherwise …} — checks that apply only under a condition. */
    public static final Capability GUARD = Capability.named("validation", "guard");

    /** {@code invariant …} — an assertion across fields. */
    public static final Capability INVARIANT = Capability.named("validation", "invariant");

    /** {@code required stop} — a failure silencing the rest of its field's checks. */
    public static final Capability STOP = Capability.named("validation", "stop");

    /** A message that is an expression rather than a literal. */
    public static final Capability MESSAGE = Capability.named("validation", "message");

    /**
     * A destination that can honour the whole language.
     *
     * @param translator what to name in a refusal
     * @return the declaration
     */
    public static Capabilities everything(String translator) {
        return Capabilities.of(translator, GATE, GUARD, INVARIANT, STOP, MESSAGE);
    }

    private JmvCapability() {
    }
}
