package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.core.Attributes;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Subject;

import java.util.Map;

public interface Authenticator extends Link<Attributes, Envelope, Authenticator.Result> {

    sealed interface Result permits Result.Success, Result.Anonymous, Result.Challenge, Result.Failure {
        record Success(Subject subject, Map<String,Object> meta) implements Result {}
        record Anonymous() implements Result {}
        record Challenge(String scheme, String realm, String message) implements Result {}
        record Failure(String reasonCode, String message) implements Result {}
    }

    @Override
    default Outcome<Result> handle(Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next) {
        return Outcome.done(authenticate(attributes, envelope, next));
    }

    Result authenticate(Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next);

}
