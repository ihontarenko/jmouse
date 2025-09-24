package org.jmouse.security.core.pipeline;

import org.jmouse.security.core.Envelope;

public interface Pipeline {
    Step execute(Envelope envelope);
}
