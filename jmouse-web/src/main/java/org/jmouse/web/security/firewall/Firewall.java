package org.jmouse.web.security.firewall;

import org.jmouse.core.Sorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Firewall {

    private final List<FirewallPolicy> policies;

    public Firewall(List<FirewallPolicy> policies) {
        policies = new ArrayList<>(policies);
        policies.sort(Sorter.PRIORITY_COMPARATOR);
        this.policies = Collections.unmodifiableList(policies);
    }

    public Decision evaluate(EvaluationInput evaluationInput) {
        Decision result = Decision.allow();

        for (FirewallPolicy policy : policies) {
            Decision decision = policy.apply(evaluationInput);
            if (decision != null && decision.action() != Decision.Action.ALLOW) {
                result = decision;
            }
        }

        return result;
    }

}
