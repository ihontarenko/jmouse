package org.jmouse.web.security.authenticator;

import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Subject;
import org.jmouse.security.core.id.Authenticator;

import java.util.Optional;

public class BasicAuthenticator implements Authenticator {

    @Override
    public Optional<Subject> authenticate(Envelope envelope) throws Exception {
        return Optional.empty();
    }

}
