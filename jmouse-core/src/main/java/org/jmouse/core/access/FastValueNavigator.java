package org.jmouse.core.access;

import static org.jmouse.core.Verify.index;
import static org.jmouse.core.Verify.nonNull;

/**
 * Optimized {@link ValueNavigator} implementation. ⚡
 *
 * <p>
 * Navigates values using {@link PropertyPath} without creating
 * intermediate sub-path objects. The navigator iterates directly
 * over {@link PropertyPath.Entries} and resolves each segment
 * through {@link ObjectAccessor}.
 * </p>
 *
 * <p>
 * Designed for performance-sensitive scenarios such as:
 * </p>
 * <ul>
 *     <li>object mapping</li>
 *     <li>form binding</li>
 *     <li>expression evaluation</li>
 *     <li>validation path resolution</li>
 * </ul>
 *
 * <p>
 * If any segment cannot be resolved, navigation stops and {@code null}
 * is returned.
 * </p>
 */
public class FastValueNavigator implements ValueNavigator {

    /**
     * Navigates using a string path representation.
     *
     * <p>
     * The path is first converted into a {@link PropertyPath}
     * and then resolved through {@link #navigate(ObjectAccessor, PropertyPath)}.
     * </p>
     *
     * @param accessor root accessor
     * @param path     property path string
     *
     * @return resolved value or {@code null}
     */
    @Override
    public Object navigate(ObjectAccessor accessor, String path) {
        nonNull(accessor, "accessor");

        if (path == null || path.isBlank()) {
            return null;
        }

        return navigate(accessor, PropertyPath.forPath(path));
    }

    /**
     * Navigates the given {@link PropertyPath}.
     *
     * @param accessor root accessor
     * @param path     property path
     *
     * @return resolved value or {@code null}
     */
    @Override
    public Object navigate(ObjectAccessor accessor, PropertyPath path) {
        return navigate(nonNull(accessor, "accessor"), nonNull(path, "path").entries(), 0);
    }

    /**
     * Navigates a property path starting from the given offset.
     *
     * @param accessor root accessor
     * @param path     property path
     * @param offset   starting segment index
     *
     * @return resolved value or {@code null}
     */
    @Override
    public Object navigate(ObjectAccessor accessor, PropertyPath path, int offset) {
        return navigate(nonNull(accessor, "accessor"), nonNull(path, "path").entries(), offset);
    }

    /**
     * Core navigation method that iterates over {@link PropertyPath.Entries}.
     *
     * <p>
     * Each path segment is resolved through {@link ObjectAccessor#get(String)}.
     * Navigation stops when:
     * </p>
     * <ul>
     *     <li>all segments are resolved — the final value is returned</li>
     *     <li>a segment cannot be resolved — {@code null} is returned</li>
     * </ul>
     *
     * @param accessor root accessor
     * @param entries  property path entries
     * @param offset   starting index within entries
     *
     * @return resolved value or {@code null}
     */
    @Override
    public Object navigate(ObjectAccessor accessor, PropertyPath.Entries entries, int offset) {
        nonNull(accessor, "accessor");

        int size = nonNull(entries, "entries").size();

        index(offset, size, "offset");

        if (offset == size) {
            return accessor;
        }

        ObjectAccessor current = accessor;

        for (int index = offset; index < size; index++) {
            current = current.get(
                    entries.get(index).toString()
            );

            if (current == null) {
                return null;
            }
        }

        return current.unwrap();
    }
}