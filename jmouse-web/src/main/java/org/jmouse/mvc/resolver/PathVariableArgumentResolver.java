package org.jmouse.mvc.resolver;

import org.jmouse.mvc.AbstractArgumentResolver;
import org.jmouse.mvc.HandlerInvocation;
import org.jmouse.mvc.MethodParameter;
import org.jmouse.mvc.RouteMatch;
import org.jmouse.mvc.mapping.annnotation.PathVariable;
import org.jmouse.web.context.WebBeanContext;

public class PathVariableArgumentResolver extends AbstractArgumentResolver {

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasAnnotation(PathVariable.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerInvocation invocation) {
        PathVariable pathVariable  = parameter.getAnnotation(PathVariable.class);
        Object       argumentValue = null;
        String       variableName  = pathVariable.value();

        if (variableName != null && !variableName.isBlank()) {
            RouteMatch match = invocation.mappedHandler().match();
            argumentValue = match.getVariable(variableName, null);
        }

        return conversion.convert(argumentValue, parameter.getJavaType().getClassType());
    }

}
