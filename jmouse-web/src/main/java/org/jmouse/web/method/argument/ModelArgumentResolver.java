package org.jmouse.web.method.argument;

import org.jmouse.mvc.*;
import org.jmouse.web.http.request.RequestAttributes;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.request.RequestContext;

import static org.jmouse.web.http.request.RequestAttributesHolder.getAttribute;

public class ModelArgumentResolver extends AbstractArgumentResolver {

    /**
     * ✅ Supports parameters of type {@link Model}.
     *
     * @param parameter the method parameter to check
     * @return true if the parameter is assignable to Model
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameterType() == Model.class;
    }

    /**
     * 📦 Returns the {@link Model} instance from the current invocation result.
     *
     * @param parameter the target parameter
     * @param requestContext   the current web request context (includes headers, session, etc.)
     * @param mappingResult the mapping result
     * @return the current Model instance
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            RequestContext requestContext,
            MappingResult mappingResult
    ) {
        if (getAttribute(RequestAttributes.MVC_RESULT_ATTRIBUTE) instanceof MVCResult mvcResult) {
            return mvcResult.getModel();
        }

        return null;
    }
}
