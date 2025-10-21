package org.jmouse.web.match.routing.condition;

import org.jmouse.core.MediaType;
import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 🎯 Matches a request's <strong>Accept</strong> header against route's <strong>produces</strong> media types.
 *
 * <p>If the request doesn't declare any {@code Accept} header, the condition matches and
 * selects an arbitrary producible (first in the set iteration order) as a default.</p>
 *
 * <p>Facets attached on hit:</p>
 * <ul>
 *   <li>{@code MediaType.class} – the negotiated producible type</li>
 *   <li>{@code ProducesMatcher.ContentNegotiation.class} – {selected, qFactor}</li>
 * </ul>
 */
public final class ProducesMatcher implements MappingMatcher<RequestRoute> {

    private final Set<MediaType> producible;

    public ProducesMatcher(Set<MediaType> producible) {
        this.producible = producible;
    }

    private static <T> T firstOf(Set<T> set) {
        Iterator<T> it = set.iterator();
        return it.hasNext() ? it.next() : null;
    }

    /**
     * Single source of truth.
     */
    @Override
    public Match apply(RequestRoute route) {
        Set<MediaType> accepted = route.accept();

        if (accepted.isEmpty()) {
            if (producible.isEmpty()) {
                return Match.hit();
            }

            MediaType selected = firstOf(producible);

            return Match.hit()
                    .attach(MediaType.class, selected)
                    .attach(Captured.class, new Captured(
                            selected, 1.0d
                    ));
        }

        double          qFactor     = -1.0d;
        MediaType       winner      = null;
        List<MediaType> intersected = MediaType.intersect(List.copyOf(producible), List.copyOf(accepted));

        if (!intersected.isEmpty()) {
            List<MediaType> prioritized = MediaType.prioritizeByQFactor(intersected);
            winner = prioritized.getFirst();
            qFactor = winner.getQFactor();
        }

        if (winner != null) {
            return Match.hit()
                    .attach(MediaType.class, winner)
                    .attach(Captured.class, new Captured(winner, qFactor));
        }

        return Match.miss();
    }

    /**
     * ⚖️ Specificity ordering:
     * 1) higher q-factor wins
     * 2) if equal, route with fewer producible types wins (narrower)
     */
    @Override
    public int compare(MappingMatcher<?> other, RequestRoute route) {
        if (!(other instanceof ProducesMatcher that)) {
            return 0;
        }

        double qA     = getGreaterQFactor(this.producible, route.accept());
        double qB     = getGreaterQFactor(that.producible, route.accept());
        int    result = Double.compare(qA, qB);

        if (result != 0) {
            return result;
        }

        return Integer.compare(that.producible.size(), this.producible.size());
    }

    /**
     * Highest q among compatible pairs.
     */
    public double getGreaterQFactor(Set<MediaType> producibleTypes, Set<MediaType> acceptedTypes) {
        double factor = 0.0d;

        for (MediaType produce : producibleTypes) {
            for (MediaType accept : acceptedTypes) {
                if (produce.compatible(accept)) {
                    factor = Math.max(factor, accept.getQFactor());
                }
            }
        }

        return factor;
    }

    @Override
    public String toString() {
        return "ProducesMatcher: " + producible;
    }

    /**
     * Facet describing the negotiation outcome.
     */
    public record Captured(MediaType selected, double qFactor) {
    }
}
