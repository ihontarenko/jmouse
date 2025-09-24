package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Subject;
import org.jmouse.security.core.id.Authenticator;

import java.util.List;
import java.util.Optional;

public final class IdentityValve implements Valve<Void> {

    private final List<Authenticator> chain;

    public IdentityValve(List<Authenticator> chain) {
        this.chain = chain;
    }

    @Override
    public Outcome<Step> handle(Void unused, Envelope envelope, Chain<Void, Envelope, Step> next) {
        Subject subject = null;

        for (Authenticator authenticator : chain) {
            Optional<Subject> optional = null;
            try {
                optional = authenticator.authenticate(envelope);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (optional.isPresent()) {
                subject = optional.get();
            }
        }

        if (subject != null) {
            envelope = envelope.withSubject(subject);
        }

        return Outcome.done(next.perform(unused, envelope));
    }

}
