package org.jmouse.security.core.id;

import org.jmouse.security.core.Envelope;
import org.jmouse.security.core.Subject;

import java.util.Optional;

public interface Authenticator {
    Optional<Subject> authenticate(Envelope envelope) throws Exception;
}
