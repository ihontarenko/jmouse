package org.jmouse.beans.resolve.resolver;

import jakarta.inject.Named;
import org.jmouse.beans.resolve.support.AnnotatedBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;

public class NamedBeanResolver extends AnnotatedBeanResolver<Named> {

    public NamedBeanResolver() {
        super(Named.class);
    }

    @Override
    protected Object resolve(BeanResolutionRequest request, Named named) {
        BeanCandidate candidate = candidates(request).getCandidate(named.value());

        if (candidate == null) {
            if (request.required()) {
                throw new IllegalStateException(
                        "Required named bean '%s' was not found".formatted(named.value())
                );
            }
            return null;
        }

        if (!request.classType().isAssignableFrom(candidate.type())) {
            throw new IllegalStateException(
                    "Named bean '%s' of type '%s' is not assignable to '%s'"
                            .formatted(named.value(), candidate.type().getName(), request.classType().getName())
            );
        }

        return candidate.bean();
    }
}