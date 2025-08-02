package org.jmouse.mvc.routing.condition;

import org.jmouse.core.MediaType;
import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.request.RequestRoute;

import java.util.Set;

/**
 * 📥 Matcher that verifies whether the request's <code>Content-Type</code>
 * matches any of the configured {@link MediaType}s.
 *
 * <p>Used in HTTP routing to support <code>@Consumes</code> or similar behavior.</p>
 *
 * <pre>{@code
 * Set<MediaType> types = Set.of(MediaType.APPLICATION_JSON);
 * MappingMatcher matcher = new ConsumesMatcher(types);
 *
 * boolean result = matcher.matches(requestRoute);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ConsumesMatcher implements MappingMatcher {

    private final Set<MediaType> consumable;

    /**
     * Creates a matcher with supported content types.
     *
     * @param consumable the set of acceptable {@link MediaType}s
     */
    public ConsumesMatcher(Set<MediaType> consumable) {
        this.consumable = consumable;
    }

    /**
     * ✅ Checks if request's content type is allowed.
     *
     * @param requestRoute current HTTP request data
     * @return true if request content type is null or included in accepted set
     */
    @Override
    public boolean matches(RequestRoute requestRoute) {
        MediaType contentType = requestRoute.contentType();

        if (contentType == null)
            return true;

        for (MediaType expected : consumable) {
            if (expected.includes(contentType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * ⚖️ Compares by number of acceptable types.
     * More specific match (smaller set) has higher priority.
     *
     * @param other         the matcher to compare to
     * @param requestRoute  current request route
     * @return comparison result
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (!(other instanceof ConsumesMatcher condition))
            return 0;

        return Integer.compare(condition.consumable.size(), this.consumable.size());
    }

    @Override
    public String toString() {
        return "ConsumesMatcher: %s".formatted(consumable);
    }
}
