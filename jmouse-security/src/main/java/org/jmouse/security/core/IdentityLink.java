package org.jmouse.security.core;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.security.core.pipeline.Identity;

import java.util.List;

abstract public class IdentityLink implements Link<Identity, Envelope, Decision> {

    protected final Chain<Attributes, Envelope, Authenticator.Result> authenticators;

    public IdentityLink(List<Authenticator> links) {
        this.authenticators = Chain.of(links, false);
    }

}
