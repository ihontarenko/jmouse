package org.jmouse.mvc.routing.condition;

import org.jmouse.core.MediaType;
import org.jmouse.mvc.routing.MappingCondition;
import org.jmouse.mvc.routing.RequestRoute;

import java.util.Set;

public class RequestConsumesCondition implements MappingCondition {

    private final Set<MediaType> consumable;

    public RequestConsumesCondition(Set<MediaType> consumable) {
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
    public MappingCondition combine(MappingCondition other) {
        if (!(other instanceof RequestConsumesCondition condition))
            return this;

        return new RequestConsumesCondition(Set.copyOf(condition.consumable));
    }

    @Override
    public int compareTo(MappingCondition other) {
        if (!(other instanceof RequestConsumesCondition condition))
            return 0;

        return Integer.compare(condition.consumable.size(), this.consumable.size());
    }
}
