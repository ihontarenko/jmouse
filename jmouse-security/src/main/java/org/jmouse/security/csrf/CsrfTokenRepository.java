package org.jmouse.security.csrf;

import org.jmouse.security.Envelope;

public interface CsrfTokenRepository {

    String load(Envelope envelope);

    String generate(Envelope envelope);

    void save(Envelope envelope, String token);

}