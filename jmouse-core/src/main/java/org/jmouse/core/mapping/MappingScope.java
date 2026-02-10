package org.jmouse.core.mapping;

import org.jmouse.core.access.PropertyPath;

/**
 * Mapping scope holder that tracks the root source object and the current property path. 🧭
 *
 * <p>{@code MappingScope} is used to propagate contextual information through nested mapping calls,
 * primarily for diagnostics and error reporting.</p>
 *
 * <p>The scope is immutable: methods such as {@link #withPath(PropertyPath)} and {@link #append(String)}
 * return new instances.</p>
 *
 * @param root root source object of the mapping invocation
 * @param path current property path within the mapping process
 */
public record MappingScope(
        Object root,
        PropertyPath path
) {

    /**
     * Create a root scope for the given root source object.
     *
     * @param rootSource root source object (may be {@code null})
     * @return root mapping scope with {@link PropertyPath#empty()}
     */
    public static MappingScope root(Object rootSource) {
        return new MappingScope(rootSource, PropertyPath.empty());
    }

    /**
     * Create a new scope with the given property path.
     *
     * @param path new property path
     * @return updated mapping scope
     */
    public MappingScope withPath(PropertyPath path) {
        return new MappingScope(root, path);
    }

    /**
     * Append a single segment to the current property path.
     *
     * @param segment path segment to append
     * @return new mapping scope with the updated path
     */
    public MappingScope append(String segment) {
        return withPath(path.append(segment));
    }
}
