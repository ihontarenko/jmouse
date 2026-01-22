package org.jmouse.core.mapping;

import org.jmouse.core.mapping.config.MappingConfig;
import org.jmouse.core.mapping.diagnostics.MappingDiagnostics;
import org.jmouse.core.mapping.runtime.MappingContext;

import static org.jmouse.core.Verify.nonNull;

/**
 * Default mapper facade. Orchestrates planning and execution.
 */
public final class DefaultObjectMapper implements ObjectMapper {

    private final MappingConfig config;

    public DefaultObjectMapper(MappingConfig config) {
        this.config = nonNull(config, "config");
    }

    @Override
    public <T> T map(Object source, Class<T> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("targetType must be non-null");
        }

        MappingDiagnostics diagnostics = new MappingDiagnostics();
        MappingContext context = new MappingContext(
                config.policy(),
                config.conversion(),
                config.virtualPropertyResolver(),
                diagnostics
        );

        // Stage 2: build MappingPlan and execute it
        // Stage 2: create target instance using ObjectFactory
        throw new UnsupportedOperationException("MappingPlan is not implemented yet (Stage 2)");
    }

    @Override
    public void map(Object source, Object target) {
        if (target == null) {
            throw new IllegalArgumentException("target must be non-null");
        }

        MappingDiagnostics diagnostics = new MappingDiagnostics();
        MappingContext context = new MappingContext(
                config.policy(),
                config.conversion(),
                config.virtualPropertyResolver(),
                diagnostics
        );

        // Stage 2: build MappingPlan for merge mapping and execute it
        throw new UnsupportedOperationException("Merge mapping is not implemented yet (Stage 2)");
    }
}
