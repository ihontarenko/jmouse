package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.security.core.*;

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
