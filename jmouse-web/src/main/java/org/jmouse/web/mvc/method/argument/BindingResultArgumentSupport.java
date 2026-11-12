package org.jmouse.web.mvc.method.argument;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.binding.BindingException;
import org.jmouse.web.binding.BindingResult;
import org.jmouse.web.http.RequestContext;

/** One argument-binding contract shared by form models and deserialized bodies. */
final class BindingResultArgumentSupport {

    private BindingResultArgumentSupport() {}

    static Object expose(BindingResult<?> result, MethodParameter parameter, RequestContext context) {
        context.request().setAttribute(BindingResult.BINDING_RESULT_ATTRIBUTE, result);
        var parameters = parameter.getParameter().getDeclaringExecutable().getParameters();
        int next = parameter.getParameterIndex() + 1;
        boolean handlesErrors =
            next < parameters.length && BindingResult.class.isAssignableFrom(parameters[next].getType());
        if (result.hasErrors() && !handlesErrors) {
            throw new BindingException(result.errors());
        }
        return result.target();
    }
}
