package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security._old.Envelope;
import org.jmouse.security._old.Subject;

import java.util.Map;

/**
 * 🛡️ Authorization step in the security pipeline.
 *
 * <p>Acts as a chain Link producing an authorization Result. The Result mirrors
 * common XACML-like outcomes: Permit, Deny, Abstain (a.k.a. NotApplicable),
 * and Indeterminate (error/uncertain). Include optional "meta" for obligations/advice.</p>
 */
public interface Authorizer extends Link<Subject, Envelope, Authorizer.Result> {

    /**
     * 🔚 Sealed authorization decision.
     */
    sealed interface Result permits Result.Permit, Result.Deny, Result.Abstain, Result.Indeterminate {
        record Permit(Map<String, Object> meta) implements Result {}
        record Deny(String reasonCode, String message, Map<String, Object> meta) implements Result {}
        record Abstain(Map<String, Object> meta) implements Result {}
        record Indeterminate(String reasonCode, String message, Throwable cause, Map<String, Object> meta) implements Result {}
    }

    @Override
    default Outcome<Result> handle(Subject subject, Envelope envelope, Chain<Subject, Envelope, Result> next) {
        return Outcome.done(authorize(subject, envelope, next));
    }

    Result authorize(Subject subject, Envelope envelope, Chain<Subject, Envelope, Result> next);
}
