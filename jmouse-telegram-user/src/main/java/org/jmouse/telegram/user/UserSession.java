package org.jmouse.telegram.user;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * What an MTProto library needs to keep one person's login: a directory for its database and a key to
 * encrypt that database with.
 *
 * <p>⚠️ <strong>The directory's contents are full control of the account.</strong> Whoever copies it is
 * signed in as that person, on every chat, with no code and no password. Hence the key, which the library
 * derives per session from a master key that lives outside any file it writes ({@link UserSessions}).
 *
 * @param name        the session's name — what an identity's credential names for a {@code USER} identity
 * @param directory   where the MTProto library keeps its database for this session
 * @param databaseKey the key that database is encrypted with — ⚠️ never logged, never returned
 */
public record UserSession(String name, Path directory, byte[] databaseKey) {

    public UserSession {
        Objects.requireNonNull(name, "session name");
        Objects.requireNonNull(directory, "session directory");
        Objects.requireNonNull(databaseKey, "database key");
        databaseKey = databaseKey.clone();
    }

    @Override
    public byte[] databaseKey() {
        return databaseKey.clone();
    }

    /** ⚠️ Redacts the key — a record's generated {@code toString} would print it into the first log line. */
    @Override
    public String toString() {
        return "UserSession[name=%s, directory=%s, databaseKey=<redacted>]".formatted(name, directory);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UserSession session
                && name.equals(session.name) && directory.equals(session.directory)
                && Arrays.equals(databaseKey, session.databaseKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, directory);
    }
}
