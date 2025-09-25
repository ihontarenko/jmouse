package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;

public class Identity extends Pipeline<Identity, IdentityLink> {

    public Identity(Chain<Identity, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Identity wrap(Chain<Identity, Envelope, Decision> chain) {
        return new Identity(chain);
    }

}
