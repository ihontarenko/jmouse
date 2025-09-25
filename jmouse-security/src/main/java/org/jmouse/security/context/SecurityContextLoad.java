package org.jmouse.security.context;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;
import org.jmouse.security.pipeline.Ingress;
import org.jmouse.security.pipeline.IngressLink;

public class SecurityContextLoad extends IngressLink {

    private final ContextPersistence persistence;

    public SecurityContextLoad(ContextPersistence persistence) {
        this.persistence = persistence;
    }

    @Override
    public Outcome<Decision> handle(Ingress ingress, Envelope envelope, Chain<Ingress, Envelope, Decision> next) {
        SecurityContext context = persistence.load(envelope);
        SecurityContextHolder.setContext(context);
        return Outcome.next();
    }

}
