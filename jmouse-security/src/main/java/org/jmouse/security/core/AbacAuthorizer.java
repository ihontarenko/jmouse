package org.jmouse.security.core;

import org.jmouse.security.core.policy.Decision;
import org.jmouse.security.core.policy.Authorizer;

import java.util.List;

public class AbacAuthorizer implements Authorizer {

    private final List<Predicate> rules;

    public AbacAuthorizer(List<Predicate> rules) {
        this.rules = rules;
    }

    @Override
    public Decision evaluate(Envelope envelope) {
        for (Predicate rule : rules) {
            if (rule.test(envelope)) {
                return Decision.permit();
            }
        }

        return Decision.abstain();
    }

    public interface Predicate {
        boolean test(Envelope env);
    }

}
