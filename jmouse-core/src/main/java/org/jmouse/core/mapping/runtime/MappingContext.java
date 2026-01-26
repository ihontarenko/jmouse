package org.jmouse.core.mapping.runtime;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.bindings.TypeMappingBindings;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.plan.PlanRegistry;

public record MappingContext(
        Mapper mapper,
        PlanRegistry planRegistry,
        ObjectAccessorWrapper wrapper,
        Conversion conversion,
        TypeMappingBindings mappingBindings,
        MappingPolicy policy
) {

    public MappingContext(
            Mapper mapper,
            PlanRegistry planRegistry,
            ObjectAccessorWrapper wrapper,
            Conversion conversion,
            TypeMappingBindings mappingBindings,
            MappingPolicy policy
    ) {
        this.mapper = Verify.nonNull(mapper, "mapper");
        this.planRegistry = Verify.nonNull(planRegistry, "planRegistry");
        this.wrapper = Verify.nonNull(wrapper, "accessorWrapper");
        this.conversion = Verify.nonNull(conversion, "conversion");
        this.mappingBindings = Verify.nonNull(mappingBindings, "mappingBindings");
        this.policy = Verify.nonNull(policy, "policy");
    }

}
