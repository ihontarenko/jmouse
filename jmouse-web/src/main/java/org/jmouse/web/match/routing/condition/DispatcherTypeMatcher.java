package org.jmouse.web.match.routing.condition;

import jakarta.servlet.DispatcherType;
import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.Objects;

/**
 * 🚦 Matches a request by its {@link DispatcherType} (REQUEST, FORWARD, INCLUDE, ASYNC, ERROR).
 *
 * <p>On hit, attaches the resolved dispatcher type as a facet so later stages can retrieve it:</p>
 * <ul>
 *   <li>{@code DispatcherType.class} – the matched dispatcher type</li>
 *   <li>{@code DispatcherTypeMatcher.Facet.class} – a small record with the same value (for readability)</li>
 * </ul>
 */
public final class DispatcherTypeMatcher implements MappingMatcher<RequestRoute> {

    private final DispatcherType dispatcherType;

    /**
     * @param dispatcherType the dispatcher type to match against
     */
    public DispatcherTypeMatcher(DispatcherType dispatcherType) {
        this.dispatcherType = Objects.requireNonNull(dispatcherType, "dispatcherType");
    }

    /**
     * Single source of truth: evaluate and attach facets on success.
     */
    @Override
    public Match apply(RequestRoute route) {
        DispatcherType actual = route.request().getDispatcherType();

        if (actual == dispatcherType) {
            return Match.hit()
                    .attach(DispatcherType.class, actual)
                    .attach(Facet.class, new Facet(actual));
        }

        return Match.miss();
    }

    /**
     * Boolean façade.
     */
    @Override
    public boolean matches(RequestRoute route) {
        return apply(route).matched();
    }

    /**
     * No ordering logic for dispatcher type by default.
     */
    @Override
    public int compare(MappingMatcher<?> other, RequestRoute route) {
        return 0;
    }

    @Override
    public String toString() {
        return "DispatcherTypeMatcher[" + dispatcherType + "]";
    }

    /**
     * Facet wrapper for clarity in downstream code.
     */
    public record Facet(DispatcherType value) {
    }
}
