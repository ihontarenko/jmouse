package org.jmouse.web.security.authenticator;

import org.jmouse.core.chain.Chain;
import org.jmouse.security.core.Attributes;
import org.jmouse.security.core.CredentialCarrier;
import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.pipeline.Authenticator;

public class BasicAuthenticator implements Authenticator {

    @Override
    public Result authenticate(Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next) {
        CredentialCarrier carrier = envelope.carrier();

        return null;
    }

}
