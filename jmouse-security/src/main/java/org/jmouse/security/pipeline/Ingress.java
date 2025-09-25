package org.jmouse.security.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;

public class Ingress extends Pipeline<Ingress, IngressLink> {

    public Ingress(Chain<Ingress, Envelope, Decision> chain) {
        super(chain);
    }

    @Override
    protected Ingress wrap(Chain<Ingress, Envelope, Decision> chain) {
        return new Ingress(chain);
    }

}
