package org.jmouse.security.core.pipeline;

import org.jmouse.security.core.Envelope;

@FunctionalInterface
public interface Valve {
    Step handle(Envelope envelope, ValveChain next) throws Exception;
}
