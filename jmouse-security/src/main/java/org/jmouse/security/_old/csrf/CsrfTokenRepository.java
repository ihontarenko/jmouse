package org.jmouse.security._old.csrf;

import org.jmouse.security._old.Envelope;

public interface CsrfTokenRepository {

    String load(Envelope envelope);

    String generate(Envelope envelope);

    void save(Envelope envelope, String token);

}