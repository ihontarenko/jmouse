package org.jmouse.core.mapping.runtime;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.mapping.diagnostics.MappingDiagnostics;
import org.jmouse.core.mapping.virtuals.VirtualPropertyResolver;

import static org.jmouse.core.Verify.nonNull;

public record MappingContext(
        MappingPolicy policy,
        Conversion conversion,
        VirtualPropertyResolver virtualPropertyResolver,
        MappingDiagnostics diagnostics
) {

    public MappingContext(
            MappingPolicy policy,
            Conversion conversion,
            VirtualPropertyResolver virtualPropertyResolver,
            MappingDiagnostics diagnostics
    ) {
        this.policy = nonNull(policy, "policy");
        this.conversion = nonNull(conversion, "conversion");
        this.virtualPropertyResolver = nonNull(virtualPropertyResolver, "virtualPropertyResolver");
        this.diagnostics = nonNull(diagnostics, "diagnostics");
    }

}
