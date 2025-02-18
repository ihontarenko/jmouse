package org.jmouse.beans.definition;

/**
 * Represents a dependency required by a structured.
 * <p>
 * A {@code BeanDependency} defines the type and optional name of the dependency,
 * which can be used to resolve it in a structured container.
 *
 * <p>Example usage:
 * <pre>{@code
 * BeanDependency dependency = new SimpleBeanDependency(UserService.class, "userService");
 * System.out.println("Dependency Type: " + dependency.type());
 * System.out.println("Dependency Name: " + dependency.name());
 * }</pre>
 */
public interface BeanDependency {

    /**
     * Gets the type of the dependency.
     *
     * @return the class type of the dependency.
     */
    Class<?> type();

    /**
     * Gets the name of the dependency.
     * <p>
     * The name may be used for resolving specific beans if multiple beans of the same type exist.
     *
     * @return the name of the dependency, or {@code null} if not specified.
     */
    String name();

}
