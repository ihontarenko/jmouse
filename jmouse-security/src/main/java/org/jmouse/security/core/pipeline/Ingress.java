package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Decision;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.IngressLink;
import org.jmouse.security.core.Pipeline;

public class Ingress extends Pipeline<Ingress, IngressLink> {

    public Ingress(Chain<Ingress, Envelope, Decision> chain) {
        super(chain);
    }

    @Override
    protected Ingress wrap(Chain<Ingress, Envelope, Decision> chain) {
        return new Ingress(chain);
    }

}
