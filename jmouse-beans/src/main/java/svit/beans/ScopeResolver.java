package svit.beans;

/**
 * Functional interface for resolving a {@link Scope} based on a given bean name.
 * <p>
 * This interface allows for dynamic determination of a bean's lifecycle scope
 * during its resolution or creation in the container.
 * </p>
 */
@FunctionalInterface
public interface ScopeResolver {

    /**
     * Resolves the {@link Scope} for a given bean name.
     *
     * @param name the name of the bean whose scope is to be resolved.
     * @return the {@link Scope} associated with the given bean name.
     */
    Scope resolveScope(String name);
}
