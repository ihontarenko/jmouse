package org.jmouse.web.match.routing.condition;

import org.jmouse.core.MediaType;
import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

import java.util.Objects;
import java.util.Set;

/**
 * 📥 Verifies whether the request's {@code Content-Type} matches any of the configured {@link MediaType}s.
 *
 * <p>Intended for HTTP routing to support {@code @Consumes}-like behavior.</p>
 *
 * <p><b>Facets attached on hit:</b></p>
 * <ul>
 *   <li>{@code MediaType.class} – the request's content type (when present)</li>
 *   <li>{@code ConsumesMatcher.Facet} – tuple of {@code requestContentType} and the {@code matchedProducible}</li>
 *   <li>{@code ConsumesMatcher.AbsentContentType} – marker facet when the request has no content type</li>
 * </ul>
 *
 * <pre>{@code
 * Set<MediaType> types = Set.of(MediaType.APPLICATION_JSON);
 * MappingMatcher<RequestRoute> matcher = new ConsumesMatcher(types);
 *
 * Match m = matcher.apply(route);
 * if (m.matched()) {
 *     m.get(ConsumesMatcher.Facet.class).ifPresent(f -> {
 *         // f.requestContentType(), f.acceptedType()
 *     });
 * }
 * }</pre>
 */
public final class ConsumesMatcher implements MappingMatcher<RequestRoute> {

    private final Set<MediaType> consumable;

    /**
     * @param consumable the set of acceptable {@link MediaType}s (non-null)
     */
    public ConsumesMatcher(Set<MediaType> consumable) {
        this.consumable = Objects.requireNonNull(consumable, "consumable");
    }

    /**
     * Single source of truth: evaluate and attach facets on success.
     */
    @Override
    public Match apply(RequestRoute route) {
        MediaType contentType = route.contentType();

        if (contentType == null) {
            return Match.hit().attach(AbsentContentType.class, AbsentContentType.INSTANCE);
        }

        for (MediaType expected : consumable) {
            if (expected.includes(contentType)) {
                return Match.hit()
                        .attach(MediaType.class, contentType)
                        .attach(Facet.class, new Facet(contentType, expected));
            }
        }

        return Match.miss();
    }

    /**
     * Boolean façade backed by {@link #apply(RequestRoute)}.
     */
    @Override
    public boolean matches(RequestRoute route) {
        return apply(route).matched();
    }

    /**
     * ⚖️ Specificity ordering: fewer acceptable types → higher priority (more specific).
     */
    @Override
    public int compare(MappingMatcher<?> other, RequestRoute route) {
        if (!(other instanceof ConsumesMatcher o)) return 0;
        // narrower wins
        return Integer.compare(o.consumable.size(), this.consumable.size());
    }

    @Override
    public String toString() {
        return "ConsumesMatcher: " + consumable;
    }

    /**
     * Marker facet for requests without Content-Type header.
     */
    public enum AbsentContentType {INSTANCE}

    /**
     * Facet carrying negotiation outcome.
     */
    public record Facet(MediaType requestContentType, MediaType acceptedType) {
    }
}
