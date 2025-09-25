package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;
import org.jmouse.security._old.Subject;

import java.util.List;
import java.util.Map;

abstract public class PolicyLink implements Link<Policy, Envelope, Decision> {

    protected final Chain<Subject, Envelope, Authorizer.Result> authorizations;

    public PolicyLink(List<Authorizer> links) {
        this.authorizations = Chain.of(links, false)
                .withFallback((c, e) -> new Authorizer.Result.Deny(
                        "deny", "deny", Map.of()));
    }

}
