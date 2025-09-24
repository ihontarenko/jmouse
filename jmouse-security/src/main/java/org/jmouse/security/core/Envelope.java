package org.jmouse.security.core;

public interface Envelope {

    Subject subject();

    Operation operation();

    Resource resource();

    Attributes attributes();

    CredentialCarrier carrier();

    Envelope withAttributes(Attributes attributes);

    Envelope withSubject(Subject subject);

}
