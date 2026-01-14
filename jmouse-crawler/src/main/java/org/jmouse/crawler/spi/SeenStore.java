package org.jmouse.crawler.spi;

import java.net.URI;

public interface SeenStore {
    boolean firstTime(URI url);
}
