package org.jmouse.security;

public record SecurityEnvelope(
        Subject subject,
        Resource resource,
        Operation operation,
        Attributes attributes,
        CredentialCarrier carrier
) implements Envelope {

    @Override
    public Envelope with(Attributes attributes) {
        return new SecurityEnvelope(subject, resource, operation, attributes, carrier);
    }

    @Override
    public Envelope with(Subject subject) {
        return new SecurityEnvelope(subject, resource, operation, attributes, carrier);
    }

}
