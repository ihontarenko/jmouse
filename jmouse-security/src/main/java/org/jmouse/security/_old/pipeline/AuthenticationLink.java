package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;

import java.util.List;

public class AuthenticationLink extends IdentityLink {

    public static final String ATTRIBUTE_LAST_CHALLENGE = "AUTH.LAST_CHALLENGE";

    public AuthenticationLink(List<Authenticator> links) {
        super(links);
    }

    @Override
    public Outcome<Decision> handle(Identity context, Envelope envelope, Chain<Identity, Envelope, Decision> next) {
        Outcome<Authenticator.Result> outcome = authenticators.proceed(envelope.attributes(), envelope);

        if (outcome instanceof Outcome.Done<Authenticator.Result>(Authenticator.Result result)) {
            switch (result) {
                case Authenticator.Result.Success success -> {
                    envelope.with(success.subject());
                    envelope.batch(success.meta());
                }
                case Authenticator.Result.Challenge challenge -> {
                    envelope.set(IdentityAttributes.ATTRIBUTE_LAST_CHALLENGE, challenge);
                    return Outcome.done(Decision.challenge(challenge.scheme(), challenge.realm(), challenge.message()));
                }
                case Authenticator.Result.Failure failure -> {
                    return Outcome.done(Decision.deny(failure.reasonCode(), failure.message()));
                }
                case Authenticator.Result.Anonymous anonymous -> {
                    return next.proceed(context, envelope);
                }
            }
        }

        return Outcome.next();
    }

}
