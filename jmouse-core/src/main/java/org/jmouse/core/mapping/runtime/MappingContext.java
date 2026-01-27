package org.jmouse.core.mapping.runtime;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.binding.TypeMappingRules;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.plan.PlanRegistry;

public record MappingContext(
        MapperProvider mapper,
        PlanRegistry planRegistry,
        ObjectAccessorWrapper wrapper,
        Conversion conversion,
        TypeMappingRules mappingRules,
        MappingPolicy policy
) {
    public MappingContext(
            MapperProvider mapper,
            PlanRegistry planRegistry,
            ObjectAccessorWrapper wrapper,
            Conversion conversion,
            TypeMappingRules mappingRules,
            MappingPolicy policy
    ) {
        this.mapper = Verify.nonNull(mapper, "mapper");
        this.planRegistry = Verify.nonNull(planRegistry, "planRegistry");
        this.wrapper = Verify.nonNull(wrapper, "accessorWrapper");
        this.conversion = Verify.nonNull(conversion, "conversion");
        this.mappingRules = Verify.nonNull(mappingRules, "mappingRules");
        this.policy = Verify.nonNull(policy, "policy");
    }

    public Mapper objectMapper() {
        return mapper.get();
    }
}
