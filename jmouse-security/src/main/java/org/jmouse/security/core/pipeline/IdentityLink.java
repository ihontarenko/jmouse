package org.jmouse.security.core.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.security.core.Attributes;
import org.jmouse.security.core.Decision;
import org.jmouse.security.core.Envelope;

import java.util.List;

abstract public class IdentityLink implements Link<Identity, Envelope, Decision> {

    protected final Chain<Attributes, Envelope, Authenticator.Result> authenticators;

    public IdentityLink(List<Authenticator> links) {
        this.authenticators = Chain.of(links, false)
                .withFallback((c, e) -> new Authenticator.Result.Anonymous());
    }

}
