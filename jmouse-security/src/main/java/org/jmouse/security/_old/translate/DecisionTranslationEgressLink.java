package org.jmouse.security._old.translate;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;
import org.jmouse.security._old.pipeline.Egress;
import org.jmouse.security._old.pipeline.EgressLink;

public class DecisionTranslationEgressLink extends EgressLink {

    private final DecisionApplier applier;

    public DecisionTranslationEgressLink(DecisionApplier applier) {
        this.applier = applier;
    }

    @Override
    public Outcome<Decision> handle(Egress egress, Envelope envelope, Chain<Egress, Envelope, Decision> next) {
        // Let downstream produce the decision first
        Outcome<Decision> out = next.proceed(egress, envelope);

        if (out instanceof Outcome.Done<Decision>(Decision decision)) {
            // Apply transport-specific mapping (HTTP)
            applier.apply(decision);
            // Keep the decision unchanged
            return Outcome.done(decision);
        }

        // No final decision produced; do nothing here.
        return Outcome.next();
    }

}
