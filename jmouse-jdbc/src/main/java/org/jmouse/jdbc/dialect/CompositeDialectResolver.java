package org.jmouse.jdbc.dialect;

import java.util.List;
import java.util.Objects;

/**
 * Resolves dialect id by trying resolvers in order.
 */
public final class CompositeDialectResolver implements DialectResolver {

    private final List<DialectResolver> resolvers;
    private final String fallbackDialectId;

    public CompositeDialectResolver(List<DialectResolver> resolvers, String fallbackDialectId) {
        this.resolvers = List.copyOf(Objects.requireNonNull(resolvers, "resolvers"));
        this.fallbackDialectId = Objects.requireNonNull(fallbackDialectId, "fallbackDialectId");
    }

    @Override
    public String resolveDialectId() {
        for (DialectResolver r : resolvers) {
            String id = safeResolve(r);
            if (id != null && !id.isBlank()) {
                return id;
            }
        }
        return fallbackDialectId;
    }

    private static String safeResolve(DialectResolver r) {
        try {
            return r.resolveDialectId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
