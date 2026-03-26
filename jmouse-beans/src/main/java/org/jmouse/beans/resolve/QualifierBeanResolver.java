package org.jmouse.beans.resolve;

import org.jmouse.beans.annotation.Qualifier;

/**
 * Resolves a bean by {@link Qualifier}. 🏷
 */
public class QualifierBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionContext context) {
        return qualifier(context) != null;
    }

    @Override
    public Object resolve(BeanResolutionContext context) {
        Qualifier     qualifier = qualifier(context);
        BeanCandidate candidate = candidates(context).getCandidate(qualifier.value());

        if (candidate == null) {
            if (context.required()) {
                throw new IllegalStateException(
                        "Required bean named '%s' was not found".formatted(qualifier.value())
                );
            }
            return null;
        }

        Class<?> expectedType = BeanTypes.rawType(context.type());

        if (!expectedType.isAssignableFrom(candidate.type())) {
            throw new IllegalStateException(
                    "Bean '%s' of type '%s' is not assignable to '%s'"
                            .formatted(candidate.name(), candidate.type().getName(), expectedType.getName())
            );
        }

        return candidate.bean();
    }

}