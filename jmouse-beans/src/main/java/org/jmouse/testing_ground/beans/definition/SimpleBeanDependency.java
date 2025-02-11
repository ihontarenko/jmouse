package org.jmouse.testing_ground.beans.definition;

/**
 * A simple implementation of the {@link BeanDependency} interface.
 * Represents a dependency required by a bean, defined by its type and optional name.
 *
 * <p>Example usage:
 * <pre>{@code
 * SimpleBeanDependency dependency = new SimpleBeanDependency(UserRepository.class, "userRepository");
 * System.out.println("Dependency type: " + dependency.type());
 * System.out.println("Dependency name: " + dependency.name());
 * }</pre>
 *
 * @param type the type of the dependency.
 * @param name the optional name of the dependency.
 */
public record SimpleBeanDependency(Class<?> type, String name) implements BeanDependency {

}
