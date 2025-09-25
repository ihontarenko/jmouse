package org.jmouse.security._old.context;

import org.jmouse.security._old.Envelope;

public interface ContextPersistence {

    SecurityContext load(Envelope envelope);

    void save(Envelope envelope, SecurityContext context);

    void clear(Envelope envelope);

}
