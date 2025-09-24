package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.core.Envelope;

public interface Valve<C> extends Link<C, Envelope, Step> {
    default Outcome<Step> proceed(Envelope envelope, ValveChain<C> next) {
        return handle(null, envelope, next);
    }
}
