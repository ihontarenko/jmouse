package org.jmouse.web.mvc.method.argument;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.binding.BindingResult;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;

import static org.jmouse.web.http.RequestAttributesHolder.getAttribute;
import static org.jmouse.web.http.RequestAttributesHolder.getRequestRoute;

/**
 * Resolves {@link BindingResult} controller arguments. 🧩
 *
 * <p>Expects a previously created {@link BindingResult} to be available
 * in request attributes, typically produced by a preceding {@code @WebModel}
 * argument resolver.</p>
 *
 * <p>This resolver is intended for request methods that support a request body.</p>
 */
public class BindingResultArgumentResolver extends AbstractArgumentResolver {

    /**
     * Checks whether the target parameter is assignable to {@link BindingResult}.
     *
     * @param parameter method parameter
     * @return {@code true} if supported
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return BindingResult.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * Resolves the current {@link BindingResult} from request attributes.
     *
     * @param parameter      target method parameter
     * @param requestContext current request context
     * @param mappingResult  current mapping result
     * @return current binding result
     *
     * @throws IllegalStateException if current HTTP method does not support request body binding
     * @throws IllegalStateException if no binding result is available in request attributes
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, RequestContext requestContext, MappingResult mappingResult) {
        BindingResult<?> bindingResult = (BindingResult<?>) getAttribute(BindingResult.BINDING_RESULT_ATTRIBUTE);
        HttpMethod       httpMethod    = getRequestRoute().method();

        if (!httpMethod.hasBody()) {
            throw new IllegalStateException(
                    "BindingResult argument is not supported for HTTP method '%s'. " +
                            "This argument can only be used for methods that carry request body or bindable form content."
                                    .formatted(httpMethod)
            );
        }

        if (bindingResult == null) {
            throw new IllegalStateException(
                    "No BindingResult found in request attributes. " +
                            "BindingResult parameter must be declared immediately after a bindable argument such as @WebModel."
            );
        }

        return bindingResult;
    }

}