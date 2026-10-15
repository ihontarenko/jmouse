package org.jmouse.git;

/**
 * How a remote is authenticated. Deliberately sealed: a transport must handle every kind, so a new kind
 * (an ssh key, later) is added here and every switch over it is told by the compiler where to look.
 *
 * <p>No implementation reveals a secret through {@link Object#toString()}, and the library never puts one
 * into a log line, an exception message or a remote address.
 */
public sealed interface GitCredentials permits GitCredentials.Anonymous, GitCredentials.AccessToken {

    /** No credentials: the remote must be readable by anyone. */
    static Anonymous anonymous() {
        return Anonymous.INSTANCE;
    }

    /**
     * An https access token sent as the password of basic authentication.
     *
     * @param username the name the host expects beside a token — GitHub accepts {@code x-access-token} (or any
     *                 name), GitLab expects {@code oauth2} for OAuth tokens
     * @param token    the secret itself
     */
    static AccessToken accessToken(String username, String token) {
        return new AccessToken(username, token);
    }

    /** Reading a public remote with no identity at all. */
    record Anonymous() implements GitCredentials {

        private static final Anonymous INSTANCE = new Anonymous();

        @Override
        public String toString() {
            return "Anonymous";
        }
    }

    /**
     * A token for https basic authentication. Only ever sent over https; the string form names the username and
     * hides the token.
     */
    record AccessToken(String username, String token) implements GitCredentials {

        public AccessToken {
            if (username == null || username.isBlank() || username.contains(":") || containsControlOrSpace(username)) {
                throw new IllegalArgumentException("An access token needs a username without spaces, colons or control characters.");
            }
            if (token == null || token.isBlank() || containsControlOrSpace(token)) {
                throw new IllegalArgumentException("An access token must be a nonblank value without spaces or control characters.");
            }
        }

        @Override
        public String toString() {
            return "AccessToken[username=" + username + ", token=***]";
        }

        private static boolean containsControlOrSpace(String value) {
            return value.chars().anyMatch(character -> Character.isWhitespace(character) || Character.isISOControl(character));
        }
    }
}
