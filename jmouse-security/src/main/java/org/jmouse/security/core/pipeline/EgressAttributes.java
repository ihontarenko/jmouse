package org.jmouse.security.core.pipeline;

/**
 * 🚪 Constants for {@link Egress} pipeline attributes.
 */
public final class EgressAttributes {

    public static final String ATTRIBUTE_RESPONSE_CODE = "EGRESS.RESPONSE_CODE";
    public static final String ATTRIBUTE_RESPONSE_SIZE = "EGRESS.RESPONSE_SIZE";
    public static final String ATTRIBUTE_RESPONSE_TIME = "EGRESS.RESPONSE_TIME";
    public static final String ATTRIBUTE_HEADERS       = "EGRESS.HEADERS";
    public static final String ATTRIBUTE_TRAILERS      = "EGRESS.TRAILERS";

    private EgressAttributes() {
    }

}
