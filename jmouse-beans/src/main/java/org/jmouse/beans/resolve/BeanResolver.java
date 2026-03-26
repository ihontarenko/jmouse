package org.jmouse.beans.resolve;

/**
 * A single bean resolution step in a resolution chain. 🔗
 */
public interface BeanResolver {

    /**
     * Checks whether this resolver can handle the context. 🔎
     *
     * @param context resolution context
     *
     * @return {@code true} if supported
     */
    boolean supports(BeanResolutionRequest context);

    /**
     * Resolves a value for the given context. ⚙️
     *
     * @param context resolution context
     *
     * @return resolved value
     */
    Object resolve(BeanResolutionRequest context);

}