package org.jmouse.security.csrf;

import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;

public interface CsrfValidator {
    Decision validate(Envelope envelope, String expectedToken);
}