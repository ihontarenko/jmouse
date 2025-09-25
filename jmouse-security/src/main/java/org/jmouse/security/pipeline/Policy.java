package org.jmouse.security.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;

public class Policy extends Pipeline<Policy, PolicyLink> {

    public Policy(Chain<Policy, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Policy wrap(Chain<Policy, Envelope, Decision> chain) {
        return new Policy(chain);
    }

}
