package org.jmouse.access.policy;

import org.jmouse.access.spi.GrantCondition;

/**
 * Turns condition source into something that can be asked — the seam an expression language plugs
 * into.
 *
 * <p>Registered by the product. `jmouse-access-el` implements it over jMouse EL; a product wanting
 * something narrower implements it differently, and one with no conditions registers nothing.
 *
 * <p>⚠️ <strong>A document containing conditions must fail to bind where no compiler is
 * registered.</strong> Not "ignore the condition" — a grant reading {@code allow if X} that grants
 * unconditionally is a hole with a comment explaining itself.
 *
 * <h2>What a restricted dialect must exclude</h2>
 *
 * <p>An expression language configurable by whitelist is the right foundation, but a function
 * whitelist alone is not enough. Two different kinds of thing have to go:
 *
 * <ul>
 *   <li><strong>Escape hatches</strong> — anything reaching a container bean, resolving a class by
 *       name, importing statics, or mutating the evaluation context. A predicate that can call an
 *       arbitrary method on an arbitrary bean bypasses every decision made above it.
 *   <li><strong>Operators that answer wrongly instead of failing</strong> — these live in the
 *       evaluator rather than the function registry, so no amount of whitelisting removes them. In a
 *       form field a silently wrong answer is cosmetic; in an authorization rule it is a hole.
 * </ul>
 *
 * <p>Comparison, boolean composition, property access, null-coalescing, the ternary and a handful of
 * tests is enough for anything an authorization predicate should say.
 */
@FunctionalInterface
public interface ConditionCompiler {

    /**
     * @throws PolicyException where the source will not compile — at load, naming the line
     */
    GrantCondition compile(String source);
}
