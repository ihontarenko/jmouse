package org.jmouse.ai.spi;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.guard.GuardedCall;

/**
 * What happened to a call.
 *
 * <p><strong>One seam, not three collaborators that are always called together.</strong> The
 * implementation this library learned from called an activity recorder, an audit recorder and a
 * metrics counter on three consecutive lines, every time, for one event — which means every adopting
 * product copies the triple or forgets part of it, and the part that gets forgotten is whichever one
 * nobody is currently looking at. One interface with four methods, and a product composes whatever it
 * likes behind it.
 *
 * <p><strong>A refusal and a failure are different and must stay so.</strong> A refusal is a decision,
 * and nothing was attempted. A failure is work that reached the domain and stopped inside it,
 * <em>possibly having done part of what it was asked</em>. Recording that second case is the only thing
 * that would ever prompt anyone to go and look at the data, and folding it into the first would bury
 * "something is broken" in the middle of the breakdown an operator reads to find out which guard is
 * firing.
 *
 * <p>Nothing here should open a transaction that the call can roll back, or throw. Bookkeeping about a
 * call that already happened must neither be undone by the work around it nor be able to fail it.
 *
 * <p>What is recorded, and in what vocabulary, is entirely a product's. The library carries
 * {@link ToolAction#traceAttributes()} for exactly this and never reads one — which is what lets a
 * product record a tool call and a human action as the same event, in its own words, without the
 * library ever learning that its domain exists.
 */
public interface InvocationTrace {

    /** A call that ran: carried out, previewed, or found to be a repeat. */
    void recordOutcome(
            CallerIdentity caller, InvocationScope scope, ToolAction action, GuardedCall guarded);

    /** A call that was refused. Nothing was changed, and the trail has to say so distinguishably. */
    void recordRefusal(
            CallerIdentity caller, InvocationScope scope, ToolAction action, ToolRefusedException refusal);

    /** A call that was attempted and threw. See above for why this is not a refusal. */
    void recordFailure(
            CallerIdentity caller, InvocationScope scope, ToolAction action, RuntimeException failure);

    /**
     * A call to a name that does not exist.
     *
     * <p>The fourth method, and it is here because the refusal happens before any {@link ToolAction}
     * exists — so it cannot go through {@link #recordRefusal} without every implementation learning to
     * expect a null action. Worth recording rather than dropping: a client calling a name that is not
     * there means a stale tool list or a hallucinated name, and it is otherwise the one failure an
     * operator cannot see, because nothing counted it against anything.
     */
    void recordUnknownAction(CallerIdentity caller, String publishedName);

    /**
     * Records nothing.
     *
     * <p>The default, so a product that has not decided what it wants observed is not made to write
     * four empty methods to say so.
     */
    static InvocationTrace none() {
        return new InvocationTrace() {

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
        };
    }
}
