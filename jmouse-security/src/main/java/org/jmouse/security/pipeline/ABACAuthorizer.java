package org.jmouse.security.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.Attributes;
import org.jmouse.security.Envelope;
import org.jmouse.security.Subject;

import java.util.List;
import java.util.Map;

public class ABACAuthorizer implements Authorizer {

    public interface Predicate {
        boolean test(Attributes attributes, Envelope envelope);
    }

    private final List<Predicate> rules;

    public ABACAuthorizer(List<Predicate> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public Result authorize(Subject subject, Envelope envelope, Chain<Subject, Envelope, Result> next) {
        for (Predicate rule : rules) {
            try {
                if (rule.test(envelope.attributes(), envelope)) {
                    return new Result.Permit(Map.of());
                }
            } catch (Exception e) {
                return new Result.Indeterminate(
                        "ABAC_ERROR", "Rule evaluation failed", e, Map.of("message", e.getMessage()));
            }
        }

        return new Result.Deny("ABAC_DENY", "No rule permitted access", Map.of());
    }
}
