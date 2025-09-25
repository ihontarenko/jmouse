package org.jmouse.web.security.authenticator;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.Attributes;
import org.jmouse.security.Envelope;
import org.jmouse.security.pipeline.Authenticator;

public class BearerJwtAuthenticator implements Authenticator {

    @Override
    public Result authenticate(Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next) {
        return null;
    }

}
