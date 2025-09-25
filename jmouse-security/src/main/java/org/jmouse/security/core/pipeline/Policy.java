package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Decision;
import org.jmouse.security.core.Envelope;

public class Policy extends Pipeline<Policy, PolicyLink> {

    public Policy(Chain<Policy, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Policy wrap(Chain<Policy, Envelope, Decision> chain) {
        return new Policy(chain);
    }

}
