package org.jmouse.beans.resolve;

/**
 * Strategy for resolving values from {@link org.jmouse.beans.BeanContext}. 🧠
 */
public interface BeanResolutionStrategy {

    /**
     * Checks whether the context can be resolved. 🔍
     *
     * @param context resolution context
     *
     * @return {@code true} if at least one resolver can resolve the context
     */
    boolean supports(BeanResolutionContext context);

    /**
     * Resolves a value for the given context. ⚙️
     *
     * @param context resolution context
     *
     * @return resolved value or {@code null} if not required and unresolved
     */
    Object resolve(BeanResolutionContext context);

}