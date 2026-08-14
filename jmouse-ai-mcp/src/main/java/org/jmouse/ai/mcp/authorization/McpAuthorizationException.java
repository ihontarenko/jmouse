package org.jmouse.ai.mcp.authorization;

/**
 * A client's attempt to authorize itself that will not be allowed to continue.
 *
 * <p>One type, because the caller here is a <strong>program</strong> and the only useful thing to do
 * with any of these is tell it what it got wrong so it can correct itself and retry. Which is also why
 * every message from this package names what would have been accepted.
 *
 * <p>⚠️ <strong>Not an authentication failure and not a permission refusal.</strong> Those are about an
 * account, and this package has no opinion about what an account is — see
 * {@link AuthorizationCodeStore}. These are refusals about the <em>protocol</em>: a challenge method
 * that is not supported, a redirect address that is not one, a verifier that does not match.
 */
public class McpAuthorizationException extends RuntimeException {

    public McpAuthorizationException(String message) {
        super(message);
    }

    public McpAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
