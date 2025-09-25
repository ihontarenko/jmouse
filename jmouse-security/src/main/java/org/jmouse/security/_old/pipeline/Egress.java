package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;

public class Egress extends Pipeline<Egress, EgressLink> {

    public Egress(Chain<Egress, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Egress wrap(Chain<Egress, Envelope, Decision> chain) {
        return new Egress(chain);
    }

}
