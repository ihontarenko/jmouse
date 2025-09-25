package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Decision;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Pipeline;

public class Egress extends Pipeline<Egress> {

    public Egress(Chain<Egress, Envelope, Decision> delegate) {
        super(delegate);
    }

    @Override
    protected Egress wrap(Chain<Egress, Envelope, Decision> chain) {
        return new Egress(chain);
    }

}
