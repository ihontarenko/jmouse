package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Decision;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.IdentityLink;
import org.jmouse.security.core.Pipeline;

public class Identity extends Pipeline<Identity, IdentityLink> {

    public Identity(Chain<Identity, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Identity wrap(Chain<Identity, Envelope, Decision> chain) {
        return new Identity(chain);
    }

}
