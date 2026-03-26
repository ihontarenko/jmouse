package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.beans.resolve.support.AnnotatedBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;

public class QualifierBeanResolver extends AnnotatedBeanResolver<Qualifier> {

    public QualifierBeanResolver() {
        super(Qualifier.class);
    }

    @Override
    protected Object resolve(BeanResolutionRequest request, Qualifier qualifier) {
        BeanCandidate candidate = candidates(request).getCandidate(qualifier.value());

        if (candidate == null) {
            if (request.required()) {
                throw new IllegalStateException(
                        "Required bean named '%s' was not found".formatted(qualifier.value())
                );
            }
            return null;
        }

        if (!request.classType().isAssignableFrom(candidate.type())) {
            throw new IllegalStateException(
                    "Bean '%s' of type '%s' is not assignable to '%s'"
                            .formatted(candidate.name(), candidate.type().getName(), request.classType().getName())
            );
        }

        return candidate.bean();
    }
}