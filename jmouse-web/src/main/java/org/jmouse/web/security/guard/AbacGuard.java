package org.jmouse.web.security.guard;

import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.policy.Decision;
import org.jmouse.security.core.policy.Guard;

import java.util.List;

public class AbacGuard implements Guard {

    private final List<Predicate> rules;

    public AbacGuard(List<Predicate> rules) {
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
