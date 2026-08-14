package org.jmouse.ai.spring;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.guard.GuardedCall;
import org.jmouse.ai.spi.CallerResolver;
import org.jmouse.ai.spi.InvocationTrace;
import org.jmouse.ai.spi.ToolAuthorizer;

/**
 * The two defaults that let an unconfigured application dispatch, wrapped so they can be recognised.
 *
 * <p>Both are needed: an application with one tool definition and no configuration has to start and work,
 * or nothing about this library can be tried out in an afternoon. Both are also exactly what must not
 * still be in place when that application reaches production — one authenticates nobody, the other
 * authorizes everything.
 *
 * <p><strong>Named types rather than the lambdas the interfaces already offer, for one reason.</strong>
 * {@link CallerResolver#anonymous()} and {@link ToolAuthorizer#permitAll()} return lambdas, and a lambda
 * cannot be told apart from a product's own two-line implementation at startup. Wrapping them lets
 * {@link AiDiagnostics} say <em>which</em> of these is still in place, by name, in the one log line
 * somebody reads. A default that is dangerous and announced is a different thing from a default that is
 * dangerous and silent.
 */
final class PermissiveDefaults {

    private PermissiveDefaults() {
    }

    /** Everybody is the same anonymous caller. */
    static final class AnonymousCallers implements CallerResolver {

        private static final CallerIdentity ANONYMOUS = CallerIdentity.of("anonymous");

        @Override
        public CallerIdentity resolve() {
            return ANONYMOUS;
        }
    }

    /** Every caller may do everything. */
    static final class PermitEverything implements ToolAuthorizer {

        @Override
        public boolean permits(CallerIdentity caller, ToolAction action) {
            return true;
        }
    }

    /**
     * Nothing is written down.
     *
     * <p>Not permissive in the way the other two are — a call still passes every gate — but the same
     * kind of default: silent, survivable in development, and the reason nobody can answer <em>"who
     * called that, and when"</em> six weeks later. Wrapped for the same reason as the other two, so the
     * startup line can say so by name instead of asking reflection what kind of anonymous class it holds.
     */
    static final class NoTrace implements InvocationTrace {

        @Override
        public void recordOutcome(
                CallerIdentity caller, InvocationScope scope, ToolAction action, GuardedCall guarded) {
        }

        @Override
        public void recordRefusal(
                CallerIdentity caller, InvocationScope scope, ToolAction action,
                ToolRefusedException refusal) {
        }

        @Override
        public void recordFailure(
                CallerIdentity caller, InvocationScope scope, ToolAction action,
                RuntimeException failure) {
        }

        @Override
        public void recordUnknownAction(CallerIdentity caller, String publishedName) {
        }
    }
}
