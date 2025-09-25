package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;

public class Policy extends Pipeline<Policy, PolicyLink> {

    public Policy(Chain<Policy, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Policy wrap(Chain<Policy, Envelope, Decision> chain) {
        return new Policy(chain);
    }

}
