package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.security._old.Attributes;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;

import java.util.List;

abstract public class IdentityLink implements Link<Identity, Envelope, Decision> {

    protected final Chain<Attributes, Envelope, Authenticator.Result> authenticators;

    public IdentityLink(List<Authenticator> links) {
        this.authenticators = Chain.of(links, false)
                .withFallback((c, e) -> new Authenticator.Result.Anonymous());
    }

}
