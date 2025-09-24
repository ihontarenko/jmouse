package org.jmouse.security.core;

public record SecurityEnvelope(
        Subject subject, Resource resource, Operation operation, Attributes attributes, CredentialCarrier carrier)
        implements Envelope {

    @Override
    public Envelope withAttributes(Attributes attributes) {
        return new SecurityEnvelope(subject, resource, operation, attributes, carrier);
    }

    @Override
    public Envelope withSubject(Subject subject) {
        return new SecurityEnvelope(subject, resource, operation, attributes, carrier);
    }

}
