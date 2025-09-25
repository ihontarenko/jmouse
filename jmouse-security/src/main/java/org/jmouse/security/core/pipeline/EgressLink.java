package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.core.Decision;
import org.jmouse.security.core.Envelope;

abstract public class EgressLink implements Link<Egress, Envelope, Decision> {

    @Override
    public Outcome<Decision> handle(Egress ingress, Envelope envelope, Chain<Egress, Envelope, Decision> next) {
        return null;
    }

}
