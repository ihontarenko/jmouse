package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.core.Envelope;

public class Pipelines {

    public static boolean run(Chain<Envelope, Void, Step> pipeline, Envelope envelope) {
        Outcome<Step> outcome = pipeline.proceed(envelope, null);

        if (outcome instanceof Outcome.Done<Step>(Step step)) {
            return step == Step.CONTINUE;
        }

        return true;
    }

}
