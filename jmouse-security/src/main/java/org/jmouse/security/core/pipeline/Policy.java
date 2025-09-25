package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Decision;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Pipeline;

public class Policy extends Pipeline<Policy> {

    public Policy(Chain<Policy, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Policy wrap(Chain<Policy, Envelope, Decision> chain) {
        return new Policy(chain);
    }

}
