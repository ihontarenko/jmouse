package org.jmouse.security.context;

import org.jmouse.security.Envelope;

public interface ContextPersistence {

    SecurityContext load(Envelope envelope);

    void save(Envelope envelope, SecurityContext context);

    void clear(Envelope envelope);

}
