package org.jmouse.security.core.pipeline;

import org.jmouse.security.core.Envelope;

public interface ValveChain {
    Step next(Envelope envelope) throws Exception;
}
