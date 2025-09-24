package org.jmouse.security.core;

import java.security.Principal;
import java.util.List;

public class AbacAuthorizer implements Authorizer {

    private final List<Predicate> rules;

    public AbacAuthorizer(List<Predicate> rules) {
        this.rules = rules;
    }

    @Override
    public Decision evaluate(Envelope envelope) {
        for (Predicate rule : rules) {
            if (rule.test(envelope) && envelope.attributes().get("principal") instanceof Principal principal) {
                // no principal here
                return Decision.permit(principal);
            }
        }

        return Decision.deny("abac deny", "DENY!");
    }

    public interface Predicate {
        boolean test(Envelope env);
    }

}
