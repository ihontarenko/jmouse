package org.jmouse.web.mvc.method.argument;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.web.binding.BindingResult;
import org.jmouse.web.binding.WebModel;
import org.jmouse.web.http.RequestAttributesHolder;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.MappingResult;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves arguments annotated with {@link WebModel}. 🧩
 *
 * <p>Binds request parameters to a target model object and exposes the
 * corresponding {@link BindingResult} in request attributes.</p>
 */
public class WebModelBindableArgumentResolver extends AbstractBindableArgumentResolver {

    /**
     * Checks whether the parameter is annotated with {@link WebModel}.
     *
     * @param parameter method parameter
     * @return {@code true} if supported
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameter().isAnnotationPresent(WebModel.class);
    }

    /**
     * Resolves the argument by binding request parameters to the target model type.
     *
     * @param parameter      target method parameter
     * @param requestContext current request context
     * @param mappingResult  current mapping result
     * @return bound model object or {@code null} if annotation metadata is unavailable
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, RequestContext requestContext, MappingResult mappingResult) {
        AnnotationRepository       repository = AnnotationRepository.ofAnnotatedElement(parameter.getParameter());
        Optional<MergedAnnotation> optional   = repository.get(WebModel.class);

        if (optional.isPresent()) {
            MergedAnnotation mergedAnnotation = optional.get();
            WebModel         webModel         = mergedAnnotation.synthesize();
            BindingResult<?> result           = dataBinder.bind(
                    getRequestParameters(), parameter.getParameterType(), webModel.value());

            attachBindingResult(result);

            return result.target();
        }

        return null;
    }

    /**
     * Stores the current {@link BindingResult} in request attributes.
     *
     * @param bindingResult binding result to expose
     */
    private void attachBindingResult(BindingResult<?> bindingResult) {
        RequestAttributesHolder.setAttribute(BindingResult.BINDING_RESULT_ATTRIBUTE, bindingResult);
    }

    /**
     * Returns current request parameters used for binding.
     *
     * @return request parameters map
     */
    private Map<String, Object> getRequestParameters() {
        return RequestAttributesHolder.getRequestParameters().parameters();
    }

}