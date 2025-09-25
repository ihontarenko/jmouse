package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;

abstract public class EgressLink implements Link<Egress, Envelope, Decision> {

    @Override
    public Outcome<Decision> handle(Egress ingress, Envelope envelope, Chain<Egress, Envelope, Decision> next) {
        return null;
    }

}
