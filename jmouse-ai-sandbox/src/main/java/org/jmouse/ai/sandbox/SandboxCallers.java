package org.jmouse.ai.sandbox;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.spi.CallerResolver;

/**
 * Two callers, and a resolver that can be switched between them.
 *
 * <p><strong>One acts on behalf of another and one acts for itself</strong>, which is the point: the
 * caller-versus-acting-subject distinction is the thing most easily asserted and least easily
 * exercised, and a sandbox with one identity would assert it without ever testing it. The assistant is
 * the service-credential shape — authorization rests on it, ownership rests on the owner — and the
 * second owner is the in-app-assistant shape, where the two identifiers are equal and no second type
 * was needed to say so.
 *
 * <p>Switching rather than reading a security context because a {@code main} has none, and because a
 * driver that can change caller between two lines is what makes the permission scenarios readable.
 */
public final class SandboxCallers implements CallerResolver {

    /** A service credential issued under {@link #OWNER}. Sees one workshop, holds everything. */
    public static final String ASSISTANT = "assistant-1";

    /** Whose parts and notes the assistant acts on. Never itself a caller here. */
    public static final String OWNER = "owner-1";

    /** A person using an in-app assistant. Sees three workshops, and deliberately lacks one permission. */
    public static final String SECOND_OWNER = "owner-2";

    /** The service-credential shape: authorization on the assistant, ownership on the owner. */
    public static final CallerIdentity ASSISTANT_FOR_OWNER =
            CallerIdentity.actingFor(ASSISTANT, OWNER);

    /** The in-app-assistant shape: one identity, both roles, and no special case for it. */
    public static final CallerIdentity PERSON = CallerIdentity.of(SECOND_OWNER);

    private CallerIdentity current = ASSISTANT_FOR_OWNER;

    @Override
    public CallerIdentity resolve() {
        return current;
    }

    public void actAs(CallerIdentity caller) {
        this.current = caller;
    }
}
