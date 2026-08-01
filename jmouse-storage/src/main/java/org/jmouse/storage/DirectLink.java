package org.jmouse.storage;

import java.net.URI;
import java.time.Duration;

/**
 * 🔗 A time-limited link that lets a client fetch bytes straight from the backend, skipping the
 * application entirely.
 *
 * <p>{@code timeToLive} is not decoration: it is how long {@code location} stays valid, so any
 * cache lifetime advertised for this link must stay below it. Caching a redirect for longer than
 * the signature it points at hands out links that are already dead.</p>
 *
 * @param location   the signed, publicly reachable URL
 * @param timeToLive how long {@code location} remains valid from now
 */
public record DirectLink(URI location, Duration timeToLive) {
}
