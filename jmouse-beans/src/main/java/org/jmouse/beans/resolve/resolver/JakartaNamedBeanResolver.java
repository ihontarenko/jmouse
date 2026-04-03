package org.jmouse.beans.resolve.resolver;

import jakarta.inject.Named;
import org.jmouse.beans.resolve.support.AnnotatedBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.core.Priority;

/**
 * {@link AnnotatedBeanResolver} implementation that resolves dependencies
 * annotated with {@link jakarta.inject.Named}. 🏷️
 *
 * <p>
 * This resolver performs name-based lookup using the value provided
 * by the {@link Named} annotation.
 * </p>
 *
 * <p><b>Resolution algorithm:</b></p>
 * <ul>
 *     <li>extract bean name from {@link Named#value()}</li>
 *     <li>lookup candidate via {@code CandidateProvider}</li>
 *     <li>validate type compatibility with requested dependency</li>
 *     <li>return resolved bean instance</li>
 * </ul>
 *
 * <p><b>Error handling:</b></p>
 * <ul>
 *     <li>if bean is not found and request is required → {@link IllegalStateException}</li>
 *     <li>if bean type is not assignable → {@link IllegalStateException}</li>
 *     <li>if bean is optional and not found → {@code null}</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * public class Service {
 *
 *     @Named("myBean")
 *     private Repository repository;
 *
 * }
 * }</pre>
 */
@Priority(Integer.MIN_VALUE + 000)
public class JakartaNamedBeanResolver extends AnnotatedBeanResolver<Named> {

    /**
     * Creates resolver for {@link Named} annotation. 🏗️
     */
    public JakartaNamedBeanResolver() {
        super(Named.class);
    }

    /**
     * Resolves a bean by name defined in {@link Named} annotation. 🔍
     *
     * @param request the resolution request context
     * @param named   the {@link Named} annotation instance
     * @return resolved bean instance or {@code null} if not required and not found
     *
     * @throws IllegalStateException if:
     * <ul>
     *     <li>required bean is missing</li>
     *     <li>resolved bean type is not assignable to requested type</li>
     * </ul>
     */
    @Override
    protected Object resolve(BeanResolutionRequest request, Named named) {
        BeanCandidate candidate = getCandidateProvider(request).getCandidate(named.value());

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