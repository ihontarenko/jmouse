package org.jmouse.security.core;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.core.pipeline.Identity;

import java.util.List;
import java.util.Optional;

public class AuthenticationLink extends IdentityLink {

    public AuthenticationLink(List<Authenticator> links) {
        super(links);
    }

    @Override
    public Outcome<Decision> handle(Identity context, Envelope envelope, Chain<Identity, Envelope, Decision> next) {
        Outcome<Authenticator.Result> outcome = authenticators.proceed(envelope.attributes(), envelope);

        if (outcome instanceof Outcome.Done<Authenticator.Result>(Authenticator.Result result)) {
            Optional<Subject> optional = Optional.empty();
            if (result instanceof Authenticator.Result.Success success) {
                optional = Optional.of(success.subject());
            }
            optional.ifPresent(envelope::withSubject);
        }

        return Outcome.done(next.perform(context, envelope));
    }

}
