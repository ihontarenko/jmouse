package org.jmouse.security._old.csrf;

import org.jmouse.security._old.Decision;
import org.jmouse.security._old.Envelope;

public interface CsrfValidator {
    Decision validate(Envelope envelope, String expectedToken);
}