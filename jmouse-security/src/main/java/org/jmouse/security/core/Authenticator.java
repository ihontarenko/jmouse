package org.jmouse.security.core;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;

import java.util.Map;
import java.util.Optional;

public interface Authenticator extends Link<Attributes, Envelope, Authenticator.Result> {

    static Success success(Subject s, Map<String, Object> meta) {
        return new Success(s, meta);
    }

    static Anonymous anonymous() {
        return new Anonymous();
    }

    static Challenge challenge(String scheme, String realm, String msg) {
        return new Challenge(scheme, realm, msg);
    }

    static Failure failure(String code, String msg) {
        return new Failure(code, msg);
    }

    static Optional<Subject> subjectOf(Result result) {
        return (result instanceof Success success) ? Optional.of(success.subject()) : Optional.empty();
    }

    default Outcome<Result> authenticate(
            Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next) throws Exception {
        Subject subject = null;

        if (handle(envelope.attributes(), envelope, next) instanceof Outcome.Done<Result>(Result result)) {
            return Outcome.done(result);
        }

        return next.proceed(attributes, envelope);
    }

    sealed interface Result permits Success, Anonymous, Challenge, Failure {

    }

    record Success(Subject subject, Map<String, Object> meta) implements Result {
        public Success {
            meta = (meta == null) ? Map.of() : Map.copyOf(meta);
        }
    }

    record Anonymous() implements Result {
    }

    record Challenge(String scheme, String realm, String message) implements Result {
    }

    record Failure(String reasonCode, String message) implements Result {
    }

}
