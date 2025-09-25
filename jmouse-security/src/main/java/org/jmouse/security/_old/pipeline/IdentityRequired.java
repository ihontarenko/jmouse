package org.jmouse.security._old.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;

import java.util.List;

public class IdentityRequired extends IdentityLink {

    public IdentityRequired(List<Authenticator> links) {
        super(links);
    }

    @Override
    public Outcome<Decision> handle(Identity identity, Envelope envelope, Chain<Identity, Envelope, Decision> next) {
        return null;
    }

}
