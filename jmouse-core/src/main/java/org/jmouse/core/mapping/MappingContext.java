package org.jmouse.core.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.ObjectAccessorWrapper;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.binding.TypeMappingRegistry;
import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.strategy.StrategyRegistry;
import org.jmouse.core.mapping.plugin.PluginBus;

public record MappingContext(
        MapperProvider mapperProvider,
        StrategyRegistry strategyRegistry,
        ObjectAccessorWrapper wrapper,
        Conversion conversion,
        TypeMappingRegistry mappingRegistry,
        MappingPolicy policy,
        MappingConfig config,
        MappingScope scope
) {

    public MappingContext(
            MapperProvider mapperProvider,
            StrategyRegistry strategyRegistry,
            ObjectAccessorWrapper wrapper,
            Conversion conversion,
            TypeMappingRegistry mappingRegistry,
            MappingPolicy policy,
            MappingConfig config,
            MappingScope scope
    ) {
        this.mapperProvider = Verify.nonNull(mapperProvider, "mapperProvider");
        this.strategyRegistry = Verify.nonNull(strategyRegistry, "strategyRegistry");
        this.wrapper = Verify.nonNull(wrapper, "accessorWrapper");
        this.conversion = Verify.nonNull(conversion, "conversion");
        this.mappingRegistry = Verify.nonNull(mappingRegistry, "mappingRegistry");
        this.policy = Verify.nonNull(policy, "policy");
        this.scope = Verify.nonNull(scope, "scope");
        this.config = Verify.nonNull(config, "config");
    }

    public MappingContext withScope(MappingScope scope) {
        return new MappingContext(mapperProvider, strategyRegistry, wrapper, conversion,
                                  mappingRegistry, policy, config, scope);
    }

    public MappingContext withPath(PropertyPath path) {
        return withScope(scope.withPath(path));
    }

    public MappingContext appendPath(String segment) {
        return withScope(scope.append(segment));
    }

    public Mapper mapper() {
        return mapperProvider.get(this);
    }

    public PluginBus plugins() {
        return new PluginBus(config().plugins());
    }

    public PropertyPath currentPath() {
        return PropertyPath.of(scope().path());
    }

}
