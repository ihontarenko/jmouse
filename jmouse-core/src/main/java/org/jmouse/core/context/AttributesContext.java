package org.jmouse.core.context;

/**
 * 🧩 Attributes-oriented context view.
 *
 * <p>
 * Semantic alias over {@link KeyValueContext}
 * for accessing contextual attributes
 * (state, metadata, execution flags).
 * </p>
 *
 * <p>
 * Does not own storage — naming clarity only.
 * </p>
 */
public interface AttributesContext extends KeyValueContext {

    /**
     * 🔍 Get attribute by name.
     *
     * @param name attribute name
     * @param <T>  expected type
     * @return attribute value or {@code null}
     */
    default <T> T getAttribute(Object name) {
        return getValue(name);
    }

    /**
     * ❗ Get required attribute.
     *
     * @param name attribute name
     * @param <T>  expected type
     * @return attribute value
     * @throws IllegalStateException if missing
     */
    default <T> T getRequiredAttribute(Object name) {
        return getRequiredValue(name);
    }

    /**
     * ❓ Check attribute presence.
     *
     * @param name attribute name
     * @return {@code true} if present
     */
    default boolean containsAttribute(Object name) {
        return containsKey(name);
    }
}
