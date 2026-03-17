package org.jmouse.core.access;

import static org.jmouse.core.Verify.index;
import static org.jmouse.core.Verify.nonNull;

/**
 * Optimized {@link ValueNavigator} implementation that can traverse
 * {@link PropertyPath.Entries} without creating sub-path instances.
 */
public class FastValueNavigator implements ValueNavigator {

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
        return navigate(nonNull(accessor, "accessor"), nonNull(path, "path").entries(), 0);
    }

    @Override
    public Object navigate(ObjectAccessor accessor, PropertyPath path, int offset) {
        return navigate(nonNull(accessor, "accessor"), nonNull(path, "path").entries(), offset);
    }

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