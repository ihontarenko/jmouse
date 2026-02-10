package org.jmouse.core.access;

/**
 * Strategy interface for resolving values from an {@link ObjectAccessor} using a textual path. 🧭
 *
 * <p>{@code ValueNavigator} is used by the mapping runtime (for example inside property mapping evaluation)
 * to resolve values using accessor-based navigation. Implementations may support simple property lookup,
 * structured path navigation, or custom navigation rules.</p>
 *
 * <p>Typical responsibilities:</p>
 * <ul>
 *   <li>interpret path syntax (e.g. {@code "name"}, {@code "address.city"}, {@code "[0]"})</li>
 *   <li>delegate navigation to {@link ObjectAccessor}</li>
 *   <li>normalize return values (unwrap nested accessors)</li>
 * </ul>
 *
 * <p>Default implementations are provided via {@link #defaultNavigator()} and {@link #simpleNavigator()}.</p>
 */
@FunctionalInterface
public interface ValueNavigator {

    /**
     * Resolve a value from {@code accessor} using the given {@code path}.
     *
     * @param accessor source accessor
     * @param path navigation path
     * @return resolved value or {@code null} when not found
     */
    Object navigate(ObjectAccessor accessor, String path);

    /**
     * Create a default navigator supporting both simple and structured paths.
     *
     * <p>Uses {@link #simpleNavigator()} for simple paths and {@link ObjectAccessor#navigate(PropertyPath)}
     * for structured paths.</p>
     *
     * @return default navigator
     */
    static ValueNavigator defaultNavigator() {
        return defaultNavigator(simpleNavigator());
    }

    /**
     * Create a default navigator using the provided simple-path navigator.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>if path is simple → delegate to {@code simple}</li>
     *   <li>otherwise → use {@link ObjectAccessor#navigate(PropertyPath)}</li>
     * </ul>
     *
     * @param simple simple-path navigator
     * @return composed navigator
     */
    static ValueNavigator defaultNavigator(ValueNavigator simple) {
        return (accessor, path) -> {
            PropertyPath propertyPath = PropertyPath.forPath(path);

            if (propertyPath.isSimple()) {
                return simple.navigate(accessor, path);
            }

            if (accessor.navigate(propertyPath) instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }

            return null;
        };
    }

    /**
     * Create a simple navigator that resolves values via {@link ObjectAccessor#get(Object)}.
     *
     * <p>This navigator does not support structured paths and should be used only for direct property access.</p>
     *
     * @return simple navigator
     */
    static ValueNavigator simpleNavigator() {
        return (accessor, path) -> {
            if (accessor.get(path) instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }
            return null;
        };
    }

}
