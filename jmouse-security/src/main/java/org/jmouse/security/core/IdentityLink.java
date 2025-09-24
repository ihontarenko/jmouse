package org.jmouse.security.core;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class IdentityLink implements Link<Pipeline<?>, Envelope, Decision> {

    private final List<Authenticator> chain;

    public IdentityLink(List<Authenticator> chain) {
        this.chain = chain;
    }

    @Override
    public Outcome<Decision> handle(
            Pipeline<?> context, Envelope envelope, Chain<Pipeline<?>, Envelope, Decision> next) {
        AtomicReference<Subject>                          subject = new AtomicReference<>();
        Chain<Attributes, Envelope, Authenticator.Result> auths   = Chain.of(this.chain, false);

        for (Authenticator authenticator : chain) {
            try {
                Outcome<Authenticator.Result> outcome = authenticator.authenticate(envelope.attributes(), envelope, auths);

                if (outcome instanceof Outcome.Done<Authenticator.Result>(Authenticator.Result result)) {
                    Optional<Subject> optional = Authenticator.subjectOf(result);
                    optional.ifPresent(subject::set);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (subject.get() != null) {
            envelope = envelope.withSubject(subject.get());
        }

        return Outcome.done(next.perform(context, envelope));
    }

}
