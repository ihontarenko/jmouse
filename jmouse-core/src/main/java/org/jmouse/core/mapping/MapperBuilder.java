package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.plan.PlanRegistry;

public final class MapperBuilder {

    private PlanRegistry          planRegistry;
    private ObjectAccessorWrapper wrapper;
    private Conversion            conversion;
    private TypeMappingRegistry   mappingRegistry;
    private MappingPolicy         policy;
    private MappingConfig         config;

    public MapperBuilder planRegistry(PlanRegistry planRegistry) {
        this.planRegistry = planRegistry;
        return this;
    }

    public MapperBuilder wrapper(ObjectAccessorWrapper wrapper) {
        this.wrapper = wrapper;
        return this;
    }

    public MapperBuilder conversion(Conversion conversion) {
        this.conversion = conversion;
        return this;
    }

    public MapperBuilder registry(TypeMappingRegistry mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
        return this;
    }

    public MapperBuilder policy(MappingPolicy policy) {
        this.policy = policy;
        return this;
    }

    public MapperBuilder config(MappingConfig config) {
        this.config = config;
        return this;
    }

    public Mapper build() {

        Verify.nonNull(planRegistry, "planRegistry");
        Verify.nonNull(wrapper, "wrapper");
        Verify.nonNull(conversion, "conversion");
        Verify.nonNull(mappingRegistry, "mappingRegistry");
        Verify.nonNull(policy, "policy");
        Verify.nonNull(config, "config");

        MappingContext context = new MappingContext(
                ObjectMapper::new,
                planRegistry,
                wrapper,
                conversion,
                mappingRegistry,
                policy,
                config,
                MappingScope.root(null)
        );

        return context.mapper();
    }
}
