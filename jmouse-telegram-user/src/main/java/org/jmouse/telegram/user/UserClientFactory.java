package org.jmouse.telegram.user;

/**
 * Opens a {@link UserClient} for a session — the one thing the product's MTProto adapter has to provide.
 */
@FunctionalInterface
public interface UserClientFactory {

    /**
     * @param session where the client keeps its database, and the key that encrypts it — ⚠️ pass the key
     *                to the MTProto library's database encryption, never to a log
     */
    UserClient open(UserSession session);
}
