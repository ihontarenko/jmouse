package org.jmouse.web.security.authenticator;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Attributes;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Authenticator;

public class BearerJwtAuthenticator implements Authenticator {

    @Override
    public Result authenticate(Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next) {
        return null;
    }

}
