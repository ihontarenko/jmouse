package org.jmouse.security.pipeline;

/**
 * 🔑 Constants for {@link Identity} (Authentication) pipeline attributes.
 */
public final class IdentityAttributes {

    public static final String ATTRIBUTE_LAST_CHALLENGE = "IDENTITY.LAST_CHALLENGE";
    public static final String ATTRIBUTE_AUTH_METHOD    = "IDENTITY.METHOD";
    public static final String ATTRIBUTE_SUBJECT        = "IDENTITY.SUBJECT";
    public static final String ATTRIBUTE_SESSION_ID     = "IDENTITY.SESSION_ID";
    public static final String ATTRIBUTE_TOKEN          = "IDENTITY.TOKEN";

    private IdentityAttributes() {
    }

}
