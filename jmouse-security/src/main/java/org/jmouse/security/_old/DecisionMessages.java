package org.jmouse.security._old;

/**
 * 💬 Default human-readable messages for {@link Decision}.
 *
 * <p>Applications can override or localize as needed.</p>
 */
public final class DecisionMessages {

    public static final String MESSAGE_UNAUTHORIZED     = "Authentication required";
    public static final String MESSAGE_BAD_CREDENTIALS  = "Invalid username or password";
    public static final String MESSAGE_TOKEN_EXPIRED    = "Session expired, please log in again";
    public static final String MESSAGE_TOKEN_INVALID    = "Invalid or malformed token";
    public static final String MESSAGE_FORBIDDEN        = "You do not have permission to access this resource";
    public static final String MESSAGE_POLICY_VIOLATION = "Request violates security policy";
    public static final String MESSAGE_SCOPE_REQUIRED   = "Insufficient permissions for requested scope";
    public static final String MESSAGE_NO_POLICY        = "No policy satisfied";
    public static final String MESSAGE_MFA_REQUIRED     = "Multi-factor authentication required";
    public static final String MESSAGE_LOGIN_REQUIRED   = "Please log in to continue";
    public static final String MESSAGE_INTERNAL_ERROR   = "Internal security error";
    public static final String MESSAGE_DEFER            = "Defer";

    private DecisionMessages() {
    }
}
