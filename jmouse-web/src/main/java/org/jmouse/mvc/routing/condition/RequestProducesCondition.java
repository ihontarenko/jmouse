package org.jmouse.mvc.routing.condition;

import org.jmouse.core.MediaType;
import org.jmouse.mvc.routing.MappingCondition;
import org.jmouse.mvc.routing.RequestRoute;

import java.util.List;
import java.util.Set;

public class RequestProducesCondition implements MappingCondition {

    private final Set<MediaType> producible;

    public RequestProducesCondition(Set<MediaType> producible) {
        this.producible = producible;
    }

    @Override
    public boolean matches(RequestRoute requestRoute) {
        List<MediaType> accepted = requestRoute.accept();

        if (accepted.isEmpty())
            return true;

        for (MediaType acceptedType : accepted) {
            for (MediaType producible : producible) {
                if (producible.includes(acceptedType)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public MappingCondition combine(MappingCondition other) {
        if (!(other instanceof RequestProducesCondition condition))
            return this;

        return new RequestProducesCondition(Set.copyOf(condition.producible));
    }

    @Override
    public int compareTo(MappingCondition other) {
        if (!(other instanceof RequestProducesCondition condition))
            return 0;

        return Integer.compare(condition.producible.size(), this.producible.size());
    }

}
