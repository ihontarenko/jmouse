package org.jmouse.security.core.policy;

import org.jmouse.security.core.Envelope;

public interface Authorizer {
    Decision evaluate(Envelope envelope);
}
