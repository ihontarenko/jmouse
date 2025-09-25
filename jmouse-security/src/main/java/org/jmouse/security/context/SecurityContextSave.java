package org.jmouse.security.context;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;
import org.jmouse.security.Subject;
import org.jmouse.security.pipeline.Egress;
import org.jmouse.security.pipeline.EgressLink;

public class SecurityContextSave extends EgressLink {

    private final ContextPersistence persistence;

    public SecurityContextSave(ContextPersistence persistence) {
        this.persistence = persistence;
    }

    @Override
    public Outcome<Decision> handle(Egress ingress, Envelope envelope, Chain<Egress, Envelope, Decision> next) {
        try {
            SecurityContext oldContext = SecurityContextHolder.getContext();
            Subject         subject    = envelope.subject() != null ? envelope.subject() : oldContext.subject();
            SecurityContext newContext = SecurityContext.of(subject, oldContext.details());
            persistence.save(envelope, newContext);
        } finally {
            SecurityContextHolder.clearContext();
        }

        return Outcome.next();
    }

}
