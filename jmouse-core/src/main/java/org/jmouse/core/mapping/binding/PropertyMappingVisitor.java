package org.jmouse.core.mapping.binding;

public interface PropertyMappingVisitor<R> {
    R visit(PropertyMapping.Ignore mapping);
    R visit(PropertyMapping.Constant mapping);
    R visit(PropertyMapping.Reference mapping);
    R visit(PropertyMapping.Provider mapping);
    R visit(PropertyMapping.Compute mapping);

    R visit(PropertyMapping.DefaultValue mapping);
    R visit(PropertyMapping.Transform mapping);
    R visit(PropertyMapping.When mapping);
    R visit(PropertyMapping.Coalesce mapping);
    R visit(PropertyMapping.Required mapping);
}
