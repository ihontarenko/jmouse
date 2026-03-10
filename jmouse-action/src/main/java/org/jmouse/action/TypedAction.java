package org.jmouse.action;

import org.jmouse.core.scope.Context;

/**
 * Typed action contract. 🎯
 *
 * @param <R> result type
 */
@FunctionalInterface
public interface TypedAction<R> {
    R execute(Context context);
}