package org.jmouse.security.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;

import java.util.List;

public class AuthorizationLink extends PolicyLink {

    public AuthorizationLink(List<Authorizer> links) {
        super(links);
    }

    @Override
    public Outcome<Decision> handle(Policy policy, Envelope envelope, Chain<Policy, Envelope, Decision> next) {
        Outcome<Authorizer.Result> outcome = authorizations.proceed(envelope.subject(), envelope);

        if (outcome instanceof Outcome.Done<Authorizer.Result>(Authorizer.Result result)) {
            return switch (result) {
                case Authorizer.Result.Permit permit
                        -> next.proceed(policy, envelope);
                case Authorizer.Result.Deny deny
                        -> Outcome.done(Decision.deny(deny.reasonCode(), deny.message()));
                case Authorizer.Result.Indeterminate indeterminate ->
                        Outcome.done(Decision.deny(indeterminate.reasonCode(), indeterminate.message()));
                case Authorizer.Result.Abstain abstain ->
                        next.proceed(policy, envelope);
            };
        }

        return Outcome.next();
    }

}
