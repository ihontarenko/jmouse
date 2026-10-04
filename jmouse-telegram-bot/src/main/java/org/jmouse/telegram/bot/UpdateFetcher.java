package org.jmouse.telegram.bot;

import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.update.Update;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * One request for updates.
 *
 * <p>Exactly {@link BotApiTransport#fetchUpdates} as a type, and it exists for one reason:
 * {@link LongPolling} holds the logic that is actually worth checking — when the offset advances, what
 * is retried, what stops the loop, how it shuts down — and none of that is reachable while the loop
 * can only be constructed around a real HTTP client.
 *
 * <p>⚠️ Deliberately <strong>not</strong> on {@code TelegramTransport}. Long polling is a Bot API
 * arrangement; MTProto pushes updates down a socket it already holds. A method on the shared SPI would
 * be one only half its implementations could mean.
 */
@FunctionalInterface
public interface UpdateFetcher {

    /**
     * @param offset         the next update id wanted — ⚠️ sending it also acknowledges everything
     *                       before it
     * @param pollTimeout    how long the server should hold the request open with nothing to say
     * @param allowedUpdates which kinds to receive, or empty for the default
     */
    List<Update> fetchUpdates(
            TelegramIdentity identity, long offset, Duration pollTimeout, Set<String> allowedUpdates);
}
