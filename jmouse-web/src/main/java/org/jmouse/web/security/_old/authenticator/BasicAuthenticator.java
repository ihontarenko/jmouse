package org.jmouse.web.security._old.authenticator;

import org.jmouse.core.chain.Chain;
import org.jmouse.security._old.Attributes;
import org.jmouse.security._old.CredentialCarrier;
import org.jmouse.security._old.Envelope;
import org.jmouse.security._old.pipeline.Authenticator;

public class BasicAuthenticator implements Authenticator {

    @Override
    public Result authenticate(Attributes attributes, Envelope envelope, Chain<Attributes, Envelope, Result> next) {
        CredentialCarrier carrier = envelope.carrier();

        return null;
    }

}
