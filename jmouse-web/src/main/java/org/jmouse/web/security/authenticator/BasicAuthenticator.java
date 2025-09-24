package org.jmouse.web.security.authenticator;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.core.Attributes;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Authenticator;

public class BasicAuthenticator implements Authenticator {

    @Override
    public Outcome<Result> handle(Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next) {
        return Outcome.done(null);
    }

}
