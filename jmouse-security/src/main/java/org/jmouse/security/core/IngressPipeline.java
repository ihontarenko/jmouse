package org.jmouse.security.core;

import org.jmouse.core.chain.Chain;

public class IngressPipeline extends Pipeline<IngressPipeline> {

    public IngressPipeline(Chain<IngressPipeline, Envelope, Decision> chain) {
        super(chain);
    }

}
