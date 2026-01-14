package org.jmouse.crawler.spi;

import java.net.URI;
import java.time.Instant;

public interface PolitenessPolicy {
    Instant notBefore(URI url, Instant now);
}
