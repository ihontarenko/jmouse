package org.jmouse.web.mvc.method.argument;

import org.jmouse.web.http.RequestAttributes;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.mvc.Model;

import static org.jmouse.web.http.RequestAttributesHolder.getAttribute;

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
     * 📦 Returns the {@link Model} instance from the current proxyInvocation result.
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
