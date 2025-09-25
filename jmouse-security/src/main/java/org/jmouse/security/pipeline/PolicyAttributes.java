package org.jmouse.security.pipeline;

/**
 * 🛡️ Constants for {@link Policy} (Authorization) pipeline attributes.
 */
public final class PolicyAttributes {

    public static final String ATTRIBUTE_POLICY_RESULT  = "POLICY.RESULT";
    public static final String ATTRIBUTE_REQUIRED_ROLE  = "POLICY.REQUIRED_ROLE";
    public static final String ATTRIBUTE_GRANTED_ROLES  = "POLICY.GRANTED_ROLES";
    public static final String ATTRIBUTE_POLICY_NAME    = "POLICY.NAME";
    public static final String ATTRIBUTE_EVALUATION_ID  = "POLICY.EVALUATION_ID";

    private PolicyAttributes() {
    }

}
