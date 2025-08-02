package org.jmouse.mvc.routing.condition;

import org.jmouse.core.MediaType;
import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.web.request.RequestRoute;

import java.util.Set;

public class ConsumesMatcher implements MappingMatcher {

    private final Set<MediaType> consumable;

    public ConsumesMatcher(Set<MediaType> consumable) {
        this.consumable = consumable;
    }

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
