package org.jmouse.access.enforcement;

import java.lang.reflect.Method;

/**
 * What to do about a call that may not proceed.
 *
 * <p>Separate from deciding, because it is a different kind of question and has more than one right
 * answer. Throwing is the obvious one; a product rolling the engine out incrementally wants to log
 * what it <em>would</em> have refused and let the call through, and one migrating a single axis wants
 * that for only some refusals. Folding any of that into the guard would put a product's rollout
 * strategy inside the mechanism.
 *
 * <p>An implementation that returns normally lets the call proceed. That is a real mode, not an
 * oversight — but it is the dangerous one, so a handler that does it should say so in a log line
 * somebody will find.
 */
@FunctionalInterface
public interface RefusalHandler {

    /** Called once per refused invocation, before the method would have run. */
    void onRefusal(AccessRefusal refusal, Method method);
}
