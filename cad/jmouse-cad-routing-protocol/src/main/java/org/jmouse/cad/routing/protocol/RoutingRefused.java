package org.jmouse.cad.routing.protocol;

/**
 * 🛤 A gesture the router refuses — parts let go where they cannot stand —, in the router's own words. Where it is answered says it
 * its own way: the server as a refusal of the request, a page as the refusal it shows.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RoutingRefused extends RuntimeException {

    public RoutingRefused(String reason) {
        super(reason);
    }
}
