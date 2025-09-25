package org.jmouse.security.pipeline;

/**
 * 🌐 Constants for {@link Ingress} pipeline attributes.
 */
public final class IngressAttributes {

    public static final String ATTRIBUTE_REQUEST_ID  = "INGRESS.REQUEST_ID";
    public static final String ATTRIBUTE_CLIENT_IP   = "INGRESS.CLIENT_IP";
    public static final String ATTRIBUTE_USER_AGENT  = "INGRESS.USER_AGENT";
    public static final String ATTRIBUTE_TIMESTAMP   = "INGRESS.TIMESTAMP";
    public static final String ATTRIBUTE_ROUTE_MATCH = "INGRESS.ROUTE_MATCH";

    private IngressAttributes() {
    }

}