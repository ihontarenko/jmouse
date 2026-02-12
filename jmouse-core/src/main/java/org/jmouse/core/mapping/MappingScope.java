package org.jmouse.core.mapping;

import org.jmouse.core.access.PropertyPath;

/**
 * Mapping scope holder that tracks the root source object, root target object,
 * and the current property path. 🧭
 *
 * <p>{@code MappingScope} is used to propagate contextual information through nested mapping calls,
 * primarily for diagnostics and error reporting.</p>
 *
 * <p>The scope is immutable: methods such as {@link #withPath(PropertyPath)} and {@link #append(String)}
 * return new instances.</p>
 *
 * @param sourceRoot root source object of the mapping invocation
 * @param targetRoot root target object of the mapping invocation (may be {@code null} early)
 * @param path current property path within the mapping process
 */
public record MappingScope(
        Object sourceRoot,
        Object targetRoot,
        PropertyPath path
) {

    /**
     * Create a root scope for the given root source and optional root target.
     *
     * @param sourceRoot root source object (may be {@code null})
     * @param targetRoot root target object (may be {@code null})
     * @return root mapping scope with {@link PropertyPath#empty()}
     */
    public static MappingScope root(Object sourceRoot, Object targetRoot) {
        return new MappingScope(sourceRoot, targetRoot, PropertyPath.empty());
    }

    /**
     * Backward-friendly factory when you only have source root.
     */
    public static MappingScope root(Object sourceRoot) {
        return root(sourceRoot, null);
    }

    /**
     * Create a new scope with the given property path.
     */
    public MappingScope withPath(PropertyPath path) {
        return new MappingScope(sourceRoot, targetRoot, path);
    }

    /**
     * Create a new scope with the given target root.
     */
    public MappingScope withTargetRoot(Object targetRoot) {
        return new MappingScope(sourceRoot, targetRoot, path);
    }

    /**
     * Append a single segment to the current property path.
     */
    public MappingScope append(String segment) {
        return withPath(path.append(segment));
    }
}
