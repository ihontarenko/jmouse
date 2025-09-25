package org.jmouse.security._old.context;

import org.jmouse.security._old.Envelope;

import java.util.Map;

public final class AttributeSecurityContextRepository implements ContextPersistence {

    public static final String SECURITY_CONTEXT_ATTRIBUTE = "SECURITY_CONTEXT";

    @Override
    public SecurityContext load(Envelope envelope) {
        Object value = envelope.get(SECURITY_CONTEXT_ATTRIBUTE);

        if (value instanceof SecurityContext securityContext) {
            return securityContext;
        }

        return SecurityContext.of(null, Map.of());
    }

    @Override
    public void save(Envelope envelope, SecurityContext context) {
        envelope.set(SECURITY_CONTEXT_ATTRIBUTE, context);
    }

    @Override
    public void clear(Envelope envelope) {
        envelope.attributes().remove(SECURITY_CONTEXT_ATTRIBUTE);
    }
}
