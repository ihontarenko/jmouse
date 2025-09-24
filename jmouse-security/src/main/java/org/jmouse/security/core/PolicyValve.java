package org.jmouse.security.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates multiple guards; deny-wins; collects obligations/advice.
 */
public class PolicyValve implements Valve {

    private final List<Authorizer> authorizers;

    public PolicyValve(List<Authorizer> authorizers) {
        this.authorizers = authorizers;
    }

    @Override
    public Step proceed(Envelope envelope, ValveChain next) throws Exception {
        boolean             anyPermit   = false;
        Map<String, Object> advice      = new LinkedHashMap<>();
        Map<String, Object> obligations = new LinkedHashMap<>();

        for (Authorizer guard : authorizers) {
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
