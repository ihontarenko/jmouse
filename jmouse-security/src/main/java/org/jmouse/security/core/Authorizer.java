package org.jmouse.security.core;

public interface Authorizer {
    Decision evaluate(Envelope envelope);
}
