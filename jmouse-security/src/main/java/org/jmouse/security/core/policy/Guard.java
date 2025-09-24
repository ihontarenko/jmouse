package org.jmouse.security.core.policy;

import org.jmouse.security.core.Envelope;

public interface Guard {
    Decision evaluate(Envelope envelope);
}
