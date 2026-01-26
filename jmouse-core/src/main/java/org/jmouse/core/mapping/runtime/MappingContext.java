package org.jmouse.core.mapping.runtime;

import org.jmouse.core.Verify;
import org.jmouse.core.convert.Conversion;
import org.jmouse.core.mapping.ObjectMapper;
import org.jmouse.core.mapping.config.MappingPolicy;
import org.jmouse.core.reflection.TypeInformation;

public final class MappingContext {

    private final ObjectMapper  mapper;
    private final Conversion    conversion;
    private final MappingPolicy policy;

    public MappingContext(ObjectMapper mapper, MappingPolicy policy, Conversion conversion) {
        this.mapper = Verify.nonNull(mapper, "mapper");
        this.policy = Verify.nonNull(policy, "policy");
        this.conversion = Verify.nonNull(conversion, "conversion");
    }

    public ObjectMapper mapper() { return mapper; }

    public MappingPolicy policy() { return policy; }

    public Conversion conversion() { return conversion; }

    public TypeInformation typeInformation(Class<?> type) {
        return TypeInformation.forClass(type);
    }

}
