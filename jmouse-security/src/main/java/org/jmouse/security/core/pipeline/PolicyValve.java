package org.jmouse.security.core.pipeline;

import org.jmouse.security.core.Attributes;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.policy.Decision;
import org.jmouse.security.core.policy.Effect;
import org.jmouse.security.core.policy.Guard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates multiple guards; deny-wins; collects obligations/advice.
 */
public class PolicyValve implements Valve {

    private final List<Guard> guards;

    public PolicyValve(List<Guard> guards) {
        this.guards = guards;
    }

    @Override
    public Step handle(Envelope envelope, ValveChain next) throws Exception {
        boolean             anyPermit   = false;
        Map<String, Object> advice      = new LinkedHashMap<>();
        Map<String, Object> obligations = new LinkedHashMap<>();

        for (Guard guard : guards) {
            Decision decision = guard.evaluate(envelope);

            if (decision == null || decision.effect() == Effect.ABSTAIN) {
                continue;
            }

            obligations.putAll(decision.obligations());
            advice.putAll(decision.advice());

            if (decision.effect() == Effect.DENY) {
                bindAttributes(envelope, obligations, advice);
                return Step.HALT;
            }

            if (decision.effect() == Effect.PERMIT) {
                anyPermit = true;
            }
        }

        bindAttributes(envelope, obligations, advice);

        return anyPermit ? next.next(envelope) : Step.HALT;
    }

    private void bindAttributes(Envelope envelope, Map<String, Object> obligations, Map<String, Object> advice) {
        Attributes attributes = envelope.attributes();
        attributes.put("advice", advice);
        attributes.put("obligations", obligations);
    }

}
