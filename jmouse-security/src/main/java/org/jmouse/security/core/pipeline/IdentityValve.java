package org.jmouse.security.core.pipeline;

import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Subject;
import org.jmouse.security.core.id.Authenticator;

import java.util.List;
import java.util.Optional;

public final class IdentityValve implements Valve {

    private final List<Authenticator> chain;

    public IdentityValve(List<Authenticator> chain) {
        this.chain = chain;
    }

    @Override
    public Step handle(Envelope envelope, ValveChain next) throws Exception {
        Subject subject = null;

        for (Authenticator authenticator : chain) {
            Optional<Subject> optional = authenticator.authenticate(envelope);
            if (optional.isPresent()) {
                subject = optional.get();
            }
        }

        if (subject != null) {
            next.next(envelope.withSubject(subject));
        }

        return next.next(envelope);
    }

}
