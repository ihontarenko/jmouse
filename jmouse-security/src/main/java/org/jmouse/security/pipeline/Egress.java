package org.jmouse.security.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;

public class Egress extends Pipeline<Egress, EgressLink> {

    public Egress(Chain<Egress, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Egress wrap(Chain<Egress, Envelope, Decision> chain) {
        return new Egress(chain);
    }

}
