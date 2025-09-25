package org.jmouse.web.security._old.authenticator;

import org.jmouse.core.chain.Chain;
import org.jmouse.security._old.Attributes;
import org.jmouse.security._old.Envelope;
import org.jmouse.security._old.pipeline.Authenticator;

public class BearerJwtAuthenticator implements Authenticator {

    @Override
    public Result authenticate(Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next) {
        return null;
    }

}
