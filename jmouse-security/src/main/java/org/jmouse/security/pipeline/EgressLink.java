package org.jmouse.security.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;

abstract public class EgressLink implements Link<Egress, Envelope, Decision> {

    @Override
    public Outcome<Decision> handle(Egress ingress, Envelope envelope, Chain<Egress, Envelope, Decision> next) {
        return null;
    }

}
