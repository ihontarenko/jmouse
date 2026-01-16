package org.jmouse.crawler.spi;

import java.net.URI;
import java.time.Instant;

public enum NoopPolitenessPolicy implements PolitenessPolicy {

    INSTANCE;

    @Override
    public Instant notBefore(URI url, Instant instant) {
        return instant;
    }
}
