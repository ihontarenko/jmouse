package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.core.Decision;
import org.jmouse.security.core.Envelope;

abstract public class IngressLink implements Link<Ingress, Envelope, Decision> {

    @Override
    public Outcome<Decision> handle(Ingress ingress, Envelope envelope, Chain<Ingress, Envelope, Decision> next) {
        return null;
    }

}
