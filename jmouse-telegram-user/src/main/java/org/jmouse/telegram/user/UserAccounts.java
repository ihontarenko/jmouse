package org.jmouse.telegram.user;

import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.update.UpdateDispatcher;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every person's account this application has open, by session name.
 *
 * <p>A client is opened the first time its session is asked for and kept until {@link #close()}. Logging
 * in goes through {@link #client(String)} — the login methods are on the client — and every other use
 * through {@link #ready(String)}, which refuses with the next login step rather than failing somewhere
 * inside MTProto.
 */
public final class UserAccounts implements AutoCloseable {

    private final UserSessions            sessions;
    private final UserClientFactory       factory;
    private final Map<String, UserClient> open = new ConcurrentHashMap<>();

    public UserAccounts(UserSessions sessions, UserClientFactory factory) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.factory  = Objects.requireNonNull(factory, "client factory");
    }

    /** The session's client, whatever its login state — what a login screen drives. */
    public UserClient client(String sessionName) {
        return open.computeIfAbsent(sessionName, name -> factory.open(sessions.session(name)));
    }

    public LoginState state(String sessionName) {
        return client(sessionName).state();
    }

    /**
     * The session's client, signed in.
     *
     * @throws TelegramException {@code Unauthorized}, whose sentence is the next login step — a screen can
     *                           show it as it is
     */
    public UserClient ready(String sessionName) {
        UserClient client = client(sessionName);
        LoginState state  = client.state();

        if (!state.isReady()) {
            throw new TelegramException(new TelegramRefusal.Unauthorized(
                    sessionName, "the account is not signed in — " + state.prompt()));
        }

        return client;
    }

    /**
     * Feeds everything the account receives — channel posts included — into the dispatcher the bots feed,
     * so reading a channel "by criteria" is the same {@code dispatcher.on(...)} a bot-fed channel uses.
     */
    public void connect(String sessionName, UpdateDispatcher dispatcher) {
        client(sessionName).listen(dispatcher::dispatch);
    }

    /** Signs out and forgets the session — the person must log in again. */
    public void logOut(String sessionName) {
        UserClient client = open.remove(sessionName);

        if (client != null) {
            client.logOut();
            client.close();
        }
    }

    @Override
    public void close() {
        open.values().forEach(UserClient::close);
        open.clear();
    }
}
