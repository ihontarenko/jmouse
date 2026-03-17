package org.jmouse.core.access;

import static org.jmouse.core.Verify.nonNull;

/**
 * Strategy interface for resolving values from an {@link ObjectAccessor} using textual
 * or structured property paths. 🧭
 *
 * <p>
 * Backward-compatible contract:
 * the original {@link #navigate(ObjectAccessor, String)} method remains the primary
 * functional method, while structured-path overloads provide a more efficient path
 * for hot execution flows.
 * </p>
 */
@FunctionalInterface
public interface ValueNavigator {

    /**
     * Resolve a value from {@code accessor} using textual path representation.
     *
     * @param accessor source accessor
     * @param path navigation path
     * @return resolved value or {@code null} when not found
     */
    Object navigate(ObjectAccessor accessor, String path);

    /**
     * Resolve a value from {@code accessor} using a pre-parsed {@link PropertyPath}.
     *
     * <p>
     * Default implementation preserves backward compatibility by delegating to
     * {@link #navigate(ObjectAccessor, String)}.
     * </p>
     *
     * @param accessor source accessor
     * @param path parsed property path
     * @return resolved value or {@code null}
     */
    default Object navigate(ObjectAccessor accessor, PropertyPath path) {
        nonNull(accessor, "accessor");
        nonNull(path, "path");

        if (path.isEmpty()) {
            return accessor;
        }

        if (path.isSimple()) {
            return navigate(accessor, path.get(0));
        }

        Object value = accessor.navigate(path);

        if (value instanceof ObjectAccessor objectAccessor) {
            return objectAccessor.unwrap();
        }

        return value;
    }

    /**
     * Resolve a value from {@code accessor} using {@link PropertyPath} starting from
     * the specified segment offset.
     *
     * <p>
     * Default implementation is backward-compatible, but may allocate due to
     * {@link PropertyPath#sub(int)}. Faster implementations may override this method.
     * </p>
     *
     * @param accessor source accessor
     * @param path parsed property path
     * @param offset starting segment index
     * @return resolved value or {@code null}
     */
    default Object navigate(ObjectAccessor accessor, PropertyPath path, int offset) {
        nonNull(accessor, "accessor");
        nonNull(path, "path");

        int size = path.size();

        if (offset < 0 || offset > size) {
            throw new IndexOutOfBoundsException("offset: " + offset + ", size: " + size);
        }

        if (offset == size) {
            return accessor;
        }

        if (offset == 0) {
            return navigate(accessor, path);
        }

        return navigate(accessor, path.sub(offset));
    }

    /**
     * Ultra-fast navigation contract using raw path entries and offset.
     *
     * <p>
     * Default implementation falls back to {@link PropertyPath} allocation.
     * Optimized navigators should override this method to avoid intermediate objects.
     * </p>
     *
     * @param accessor source accessor
     * @param entries parsed path entries
     * @param offset starting segment index
     * @return resolved value or {@code null}
     */
    default Object navigate(ObjectAccessor accessor, PropertyPath.Entries entries, int offset) {
        nonNull(entries, "entries");
        return navigate(accessor, PropertyPath.of(entries), offset);
    }

    /**
     * Create a default navigator supporting both simple and structured paths.
     *
     * @return default navigator
     */
    static ValueNavigator defaultNavigator() {
        return defaultNavigator(simpleNavigator());
    }

    /**
     * Create a default navigator using the provided simple-path navigator.
     *
     * @param simple simple-path navigator
     * @return composed navigator
     */
    static ValueNavigator defaultNavigator(ValueNavigator simple) {
        nonNull(simple, "simple");

        return new ValueNavigator() {

            @Override
            public Object navigate(ObjectAccessor accessor, String path) {
                nonNull(accessor, "accessor");

                if (path == null || path.isBlank()) {
                    return null;
                }

                return navigate(accessor, PropertyPath.forPath(path));
            }

            @Override
            public Object navigate(ObjectAccessor accessor, PropertyPath path) {
                nonNull(accessor, "accessor");
                nonNull(path, "path");

                if (path.isEmpty()) {
                    return accessor;
                }

                if (path.isSimple()) {
                    return simple.navigate(accessor, path.get(0));
                }

                Object value = accessor.navigate(path);

                if (value instanceof ObjectAccessor objectAccessor) {
                    return objectAccessor.unwrap();
                }

                return value;
            }
        };
    }

    /**
     * Create a simple navigator that resolves values via {@link ObjectAccessor#get(Object)}.
     *
     * @return simple navigator
     */
    static ValueNavigator simpleNavigator() {
        return (accessor, path) -> {
            nonNull(accessor, "accessor");

            if (path == null || path.isBlank()) {
                return null;
            }

            Object value = accessor.get(path);

            if (value instanceof ObjectAccessor objectAccessor) {
                return objectAccessor.unwrap();
            }

            return value;
        };
    }

}