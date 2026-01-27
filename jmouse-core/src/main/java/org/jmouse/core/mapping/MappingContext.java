package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.plan.PlanRegistry;

public record MappingContext(
        MapperProvider mapperProvider,
        PlanRegistry planRegistry,
        ObjectAccessorWrapper wrapper,
        Conversion conversion,
        TypeMappingRegistry mappingRegistry,
        MappingPolicy policy
) {

    public MappingContext(
            MapperProvider mapperProvider,
            PlanRegistry planRegistry,
            ObjectAccessorWrapper wrapper,
            Conversion conversion,
            TypeMappingRegistry mappingRegistry,
            MappingPolicy policy
    ) {
        this.mapperProvider = Verify.nonNull(mapperProvider, "mapperProvider");
        this.planRegistry = Verify.nonNull(planRegistry, "planRegistry");
        this.wrapper = Verify.nonNull(wrapper, "accessorWrapper");
        this.conversion = Verify.nonNull(conversion, "conversion");
        this.mappingRegistry = Verify.nonNull(mappingRegistry, "mappingRegistry");
        this.policy = Verify.nonNull(policy, "policy");
    }

    public Mapper mapper() {
        return mapperProvider.get();
    }

}
