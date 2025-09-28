package org.jmouse.web.match.routing.condition;

import org.jmouse.core.MediaType;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.http.request.RequestRoute;

import java.util.Set;

/**
 * 🎯 Matches a request's <strong>Accept</strong> header against route's <strong>produces</strong> media types.
 *
 * <p>If the request doesn't declare any {@code Accept} header, the condition always matches.</p>
 *
 * <p>Used internally to filter and prioritize routes based on content negotiation.</p>
 *
 * <p>Example: if a route declares {@code produces = [application/json]}, and
 * the client sends {@code Accept: application/json}, this matcher succeeds.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class ProducesMatcher implements MappingMatcher {

    private final Set<MediaType> producible;

    /**
     * @param producible media types that a route can produce (e.g. JSON, HTML)
     */
    public ProducesMatcher(Set<MediaType> producible) {
        this.producible = producible;
    }

    /**
     * 🔍 Checks if any of the request's accepted types match the route's producible types.
     *
     * @param requestRoute request metadata
     * @return {@code true} if any {@code Accept} type matches a {@code produces} type, or if no {@code Accept} present
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        Set<MediaType> accepted = requestRoute.accept();

        if (accepted.isEmpty()) {
            return true;
        }

        for (MediaType acceptedType : accepted) {
            for (MediaType producible : producible) {
                if (producible.compatible(acceptedType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * ⚖️ Compares specificity between two {@code produces} conditions.
     *
     * <p>More specific means:</p>
     * <ul>
     *     <li>Higher q-factor match wins</li>
     *     <li>If q-factors equal, narrower set wins (less general)</li>
     * </ul>
     *
     * @param other matcher to compare with
     * @param requestRoute request metadata
     * @return comparison result (-1, 0, 1)
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof ProducesMatcher that)) {
            return 0;
        }

        double qFactorA = getGreaterQFactor(this.producible, requestRoute.accept());
        double qFactorB = getGreaterQFactor(that.producible, requestRoute.accept());

        int result = Double.compare(qFactorA, qFactorB);

        if (result != 0) {
            return result;
        }

        return Integer.compare(that.producible.size(), this.producible.size());
    }

    /**
     * 🧮 Calculates highest quality (q-factor) match between producible and accepted types.
     */
    public double getGreaterQFactor(Set<MediaType> producibleTypes, Set<MediaType> acceptedTypes) {
        double qFactor = 0.0D;

        for (MediaType producibleType : producibleTypes) {
            for (MediaType acceptedType : acceptedTypes) {
                if (producibleType.includes(acceptedType)) {
                    qFactor = Math.max(qFactor, acceptedType.getQFactor());
                }
            }
        }

        return qFactor;
    }

    @Override
    public String toString() {
        return "ProducesMatcher: %s".formatted(producible);
    }

}
