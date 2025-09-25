package org.jmouse.security;

/**
 * 📑 Standardized reason codes and error identifiers for {@link Decision}.
 *
 * <p>These codes are machine-friendly and should be stable for programmatic checks.
 * Messages can still be overridden for user-facing output.</p>
 */
public final class DecisionCodes {

    // 🔑 Authentication / Identity
    public static final String UNAUTHORIZED       = "UNAUTHORIZED";       // no credentials
    public static final String BAD_CREDENTIALS    = "BAD_CREDENTIALS";    // invalid username/password
    public static final String TOKEN_EXPIRED      = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID      = "TOKEN_INVALID";
    public static final String UNSUPPORTED_SCHEME = "UNSUPPORTED_SCHEME";
    // 🛡️ Authorization / Policy
    public static final String NO_POLICY          = "NO_POLICY";          // no policies
    public static final String FORBIDDEN          = "FORBIDDEN";          // access denied
    public static final String POLICY_VIOLATION   = "POLICY_VIOLATION";   // failed ABAC/RBAC rule
    public static final String INSUFFICIENT_SCOPE = "INSUFFICIENT_SCOPE"; // OAuth2-like scopes
    public static final String INDETERMINATE      = "INDETERMINATE";      // could not evaluate
    // 🌐 Challenges
    public static final String CHALLENGE    = "CHALLENGE";
    public static final String MFA_REQUIRED = "MFA_REQUIRED";
    // 🔀 Redirects
    public static final String REDIRECT       = "REDIRECT";
    public static final String LOGIN_REQUIRED = "LOGIN_REQUIRED"; // typical web case
    // ⚠️ Generic / Technical
    public static final String ERROR               = "ERROR";
    public static final String INTERNAL_ERROR      = "INTERNAL_ERROR";
    public static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";

    private DecisionCodes() {
    }
}
