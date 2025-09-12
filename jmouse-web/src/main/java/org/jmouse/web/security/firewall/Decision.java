package org.jmouse.web.security.firewall;

import org.jmouse.web.http.HttpStatus;

import static org.jmouse.web.http.HttpStatus.OK;

/**
 * 🛡️ Represents the result of a firewall evaluation.
 *
 * <p>Encapsulates an {@link Action}, an {@link HttpStatus}, and an optional reason.</p>
 *
 * <p>💡 Static factories are provided to easily create {@code allow}, {@code block},
 * and {@code challenge} decisions.</p>
 *
 * @param action     the decision action
 * @param httpStatus the HTTP status to apply if blocked or challenged
 * @param reason     human-readable reason for the decision
 */
public record Decision(Action action, HttpStatus httpStatus, String reason) {

    /**
     * ❌ @return {@code true} if the request is not allowed (i.e. blocked or challenged).
     */
    public boolean isNotAllowed() {
        return action != Action.ALLOW;
    }

    /**
     * 🚫 @return {@code true} if the decision is a block.
     *
     * <p>⚠️ Note: Current implementation always returns {@code false}
     * due to using {@code !isNotAllowed()} — may need to be fixed to
     * {@code action == Action.BLOCK}.</p>
     */
    public boolean isBlocked() {
        return !isNotAllowed();
    }

    /**
     * ✅ Create an "allow" decision (HTTP 200 OK).
     */
    public static Decision allow() {
        return new Decision(Action.ALLOW, OK, OK.getText());
    }

    /**
     * 🚫 Create a "block" decision with status and reason.
     *
     * @param status HTTP status
     * @param reason reason string
     */
    public static Decision block(HttpStatus status, String reason) {
        return new Decision(Action.BLOCK, status, reason);
    }

    /**
     * 🔐 Create a "challenge" decision with status and reason.
     *
     * @param status HTTP status
     * @param reason reason string
     */
    public static Decision challenge(HttpStatus status, String reason) {
        return new Decision(Action.CHALLENGE, status, reason);
    }

    /**
     * 🚫 Create a "block" decision using raw HTTP status code.
     *
     * @param status HTTP status code
     * @param reason reason string
     */
    public static Decision block(int status, String reason) {
        return new Decision(Action.BLOCK, HttpStatus.ofCode(status), reason);
    }

    /**
     * 🔐 Create a "challenge" decision using raw HTTP status code.
     *
     * @param status HTTP status code
     * @param reason reason string
     */
    public static Decision challenge(int status, String reason) {
        return new Decision(Action.CHALLENGE, HttpStatus.ofCode(status), reason);
    }

    /**
     * 🎭 Types of firewall actions.
     */
    public enum Action {ALLOW, CHALLENGE, BLOCK}
}
