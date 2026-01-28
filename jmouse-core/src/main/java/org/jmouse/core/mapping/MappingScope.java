package org.jmouse.core.mapping;

import org.jmouse.core.bind.PropertyPath;

public record MappingScope(
        Object root,
        PropertyPath path
) {
    public static MappingScope root(Object rootSource) {
        return new MappingScope(rootSource, PropertyPath.empty());
    }

    public MappingScope withPath(PropertyPath path) {
        return new MappingScope(root, path);
    }

    public MappingScope append(String segment) {
        return withPath(path.append(segment));
    }
}
