package org.jmouse.mapper;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.convert.Conversion;
import org.jmouse.mapper.binding.TypeMappingRegistry;
import org.jmouse.mapper.config.MappingConfig;
import org.jmouse.mapper.config.MappingPolicy;
import org.jmouse.mapper.strategy.StrategyRegistry;
import org.jmouse.mapper.plugin.PluginBus;

public record MappingContext(
        MapperProvider mapperProvider,
        StrategyRegistry strategyRegistry,
        AccessorWrapper wrapper,
        Conversion conversion,
        TypeMappingRegistry mappingRegistry,
        MappingPolicy policy,
        MappingConfig config,
        MappingScope scope
) {

    public MappingContext(
            MapperProvider mapperProvider,
            StrategyRegistry strategyRegistry,
            AccessorWrapper wrapper,
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
        return new MappingContext(
                mapperProvider,
                strategyRegistry,
                wrapper,
                conversion,
                mappingRegistry,
                policy,
                config,
                scope
        );
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
        return config().pluginBus();
    }

    /**
     * The path reached so far.
     *
     * <p>Returned as held rather than copied: {@link PropertyPath} is immutable, and this is asked
     * once per property of every mapped object.</p>
     *
     * @return current property path
     */
    public PropertyPath currentPath() {
        return scope().path();
    }

}
