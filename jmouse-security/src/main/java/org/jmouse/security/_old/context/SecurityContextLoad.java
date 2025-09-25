package org.jmouse.security._old.context;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;
import org.jmouse.security._old.pipeline.Ingress;
import org.jmouse.security._old.pipeline.IngressLink;

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
