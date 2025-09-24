package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Envelope;

public interface ValveChain<C> extends Chain<C, Envelope, Step> {
    Step next(Envelope envelope) throws Exception;
}
