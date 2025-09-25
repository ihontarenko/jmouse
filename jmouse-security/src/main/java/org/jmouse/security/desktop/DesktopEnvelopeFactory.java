package org.jmouse.security.desktop;

import org.jmouse.security.*;

import java.util.Map;

public final class DesktopEnvelopeFactory {

    public static Envelope forMethod(String methodId, Map<String,Object> args, Map<String,Object> environment) {
        Attributes attributes = Attributes.mutable();
        Resource   resource   = SecurityResource.of(methodId, "method", Map.of("arguments", args.keySet()));
        Operation  operation  = SecurityOperation.of("INVOKE", Map.of("argumentsCount", args.size()));

        environment.forEach(attributes::set);

        return new SecurityEnvelope(Subjects.anonymous(), resource, operation, attributes, new LocalCarrier());
    }

    /**
     * Local process has no headers; we can expose System props/env vars as carrier if needed.
     */
    static final class LocalCarrier implements CredentialCarrier {

        @Override
        public String first(String key) {
            return null;
        }

        @Override
        public Map<String, String> all() {
            return Map.of();
        }

    }

}
