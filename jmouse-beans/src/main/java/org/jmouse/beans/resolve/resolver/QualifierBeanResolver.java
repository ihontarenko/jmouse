package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.beans.resolve.support.AnnotatedBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;

/**
 * Resolves dependencies annotated with {@link Qualifier}. 🏷️
 *
 * <p>Performs name-based lookup and validates type compatibility.</p>
 */
public class QualifierBeanResolver extends AnnotatedBeanResolver<Qualifier> {

    /**
     * Creates resolver for {@link Qualifier}.
     */
    public QualifierBeanResolver() {
        super(Qualifier.class);
    }

    /**
     * Resolves a bean by qualifier value.
     *
     * @param request   resolution request
     * @param qualifier qualifier annotation
     * @return resolved bean or {@code null} if not required and not found
     */
    @Override
    protected Object resolve(BeanResolutionRequest request, Qualifier qualifier) {
        BeanCandidate candidate = getCandidateProvider(request).getCandidate(qualifier.value());

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