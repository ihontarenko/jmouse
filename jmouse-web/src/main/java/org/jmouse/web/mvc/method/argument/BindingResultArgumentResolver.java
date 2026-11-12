package org.jmouse.web.mvc.method.argument;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.annotation.RequestBody;
import org.jmouse.web.binding.BindingResult;
import org.jmouse.web.binding.WebModel;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;

/** The result belongs to the immediately preceding form/model or body argument, including GET query binding. */
public class BindingResultArgumentResolver extends AbstractArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return BindingResult.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, RequestContext context, MappingResult mapping) {
        var parameters = parameter.getParameter().getDeclaringExecutable().getParameters();
        int previous = parameter.getParameterIndex() - 1;
        if (
            previous < 0 ||
            !(
                parameters[previous].isAnnotationPresent(WebModel.class) ||
                parameters[previous].isAnnotationPresent(RequestBody.class)
            )
        ) {
            throw new IllegalStateException(
                "BindingResult must immediately follow a WebModel or RequestBody argument"
            );
        }
        Object result = context.request().getAttribute(BindingResult.BINDING_RESULT_ATTRIBUTE);
        if (!(result instanceof BindingResult<?>)) {
            throw new IllegalStateException("No binding result was produced for the preceding argument");
        }
        return result;
    }
}
