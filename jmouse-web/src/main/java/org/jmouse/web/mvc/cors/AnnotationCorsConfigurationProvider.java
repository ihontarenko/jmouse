package org.jmouse.web.mvc.cors;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.web.mvc.MappedHandler;
import org.jmouse.web.mvc.method.HandlerMethod;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * Resolves effective {@link CorsConfiguration} from {@link CorsMapping} annotations
 * present on a handler method and/or its declaring class.
 *
 * <p>Precedence: method-level mapping overrides/augments class-level mapping
 * (the two configurations are merged).</p>
 *
 * <p>Intended to be used by the CORS layer before request processing.</p>
 *
 * @see CorsMapping
 * @see CorsConfiguration
 */
public class AnnotationCorsConfigurationProvider implements CorsConfigurationProvider {

    /**
     * Converts an annotation into a mutable {@link CorsConfiguration}.
     */
    private static CorsConfiguration toCorsConfiguration(CorsMapping annotation) {
        return new CorsConfiguration().allowedOrigins(annotation.origins()).allowedMethods(annotation.methods())
                .allowedHeaders(annotation.allowedHeaders()).exposedHeaders(annotation.exposedHeaders())
                .allowCredentials(annotation.allowCredentials()).maxAge(annotation.maxAge());
    }

    /**
     * Finds a method-level {@link CorsMapping} on the handler method, if any.
     */
    private static CorsMapping findHandlerMethod(Object handler) {
        HandlerMethod handlerMethod = getHandlerMethod(handler);
        if (handlerMethod != null) {
            return extractCorsMapping(handlerMethod.getMethod());
        }
        return null;
    }

    /**
     * Finds a class-level {@link CorsMapping} on the handler’s declaring class, if any.
     */
    private static CorsMapping findHandlerGlobal(Object handler) {
        HandlerMethod handlerMethod = getHandlerMethod(handler);
        if (handlerMethod != null) {
            return extractCorsMapping(handlerMethod.getDescriptor().unwrap().getDeclaringClass());
        }
        return null;
    }

    /**
     * Extracts a {@link CorsMapping} from the annotated element, respecting merged/meta-annotations.
     */
    private static CorsMapping extractCorsMapping(AnnotatedElement annotatedElement) {
        AnnotationRepository       repository  = AnnotationRepository.ofAnnotatedElement(annotatedElement);
        Optional<MergedAnnotation> corsMapping = repository.get(CorsMapping.class);
        return corsMapping.map(merged -> merged.getNativeAnnotation(CorsMapping.class)).orElse(null);
    }

    /**
     * Returns the {@link HandlerMethod} if the given handler wraps one; otherwise {@code null}.
     */
    private static HandlerMethod getHandlerMethod(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod;
        }

        if (handler instanceof MappedHandler mappedHandler && mappedHandler.handler() instanceof HandlerMethod handlerMethod) {
            return handlerMethod;
        }

        return null;
    }

    /**
     * Resolves CORS configuration from the handler’s annotations.
     *
     * <p>When both method- and class-level mappings are present, the result is
     * {@link Cors#combine(CorsConfiguration, CorsConfiguration)
     * combine}(class, method).</p>
     *
     * @param request current servlet request (not used here)
     * @param handler selected handler (e.g., {@link HandlerMethod})
     * @return effective configuration, or {@code null} if no mappings found
     */
    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request, Object handler) {
        CorsMapping methodAnnotation = findHandlerMethod(handler);
        CorsMapping globalAnnotation = findHandlerGlobal(handler);

        if (methodAnnotation == null && globalAnnotation == null) {
            return null;
        }

        CorsConfiguration globalConfiguration = (globalAnnotation == null) ? null : toCorsConfiguration(
                globalAnnotation);
        CorsConfiguration methodConfiguration = (methodAnnotation == null) ? null : toCorsConfiguration(
                methodAnnotation);

        if (methodConfiguration != null && globalConfiguration != null) {
            return Cors.combine(globalConfiguration, methodConfiguration);
        }

        return (methodConfiguration != null) ? methodConfiguration : globalConfiguration;
    }
}
