package org.jmouse.beans;

/**
 * Marker for bean instantiation strategy. 🧩
 *
 * <p>Used to match {@code BeanDefinition} with appropriate instantiation logic.</p>
 */
public interface InstantiationType {

    /**
     * Checks whether this type matches another instantiation type.
     *
     * @param type other instantiation type
     * @return {@code true} if same
     */
    default boolean isTheSame(InstantiationType type) {
        return type == this || this.equals(type);
    }

}