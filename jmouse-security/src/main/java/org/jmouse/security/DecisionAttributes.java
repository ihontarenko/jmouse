package org.jmouse.security;

/**
 * 🧩 Standard attribute keys used inside {@link Decision#attributes()}.
 *
 * <p>Values are stored as strings or objects, depending on context.</p>
 */
public final class DecisionAttributes {

    private DecisionAttributes() {}

    // 🌐 Challenge / WWW-Authenticate
    public static final String SCHEME = "SCHEME";
    public static final String REALM  = "REALM";
    public static final String NONCE  = "NONCE";
    public static final String SCOPE  = "SCOPE";

    // 🔀 Redirects
    public static final String LOCATION  = "LOCATION";
    public static final String RETURN_TO = "RETURN_TO";

    // 🧑 Principal / Claims
    public static final String CLAIMS      = "CLAIMS";
    public static final String ROLES       = "ROLES";
    public static final String PERMISSIONS = "PERMISSIONS";

    // ⚙️ Metadata
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String TRACE_ID  = "TRACE_ID";
}
