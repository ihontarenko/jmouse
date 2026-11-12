package org.jmouse.web.mvc.method.argument;

import java.util.Map;
import java.util.Objects;
import org.jmouse.core.MethodParameter;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.web.binding.WebModel;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.RequestParameters;
import org.jmouse.web.mvc.MappingResult;

/** Binds request/form parameters, preserving explicit BindingResult handling and rejecting unhandled errors. */
public class WebModelBindableArgumentResolver extends AbstractBindableArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameter().isAnnotationPresent(WebModel.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, RequestContext context, MappingResult mapping) {
        WebModel annotation = parameter.getParameter().getAnnotation(WebModel.class);
        Object source = RequestParameters.ofRequest(context.request()).parameters();
        if (!annotation.prefix().isBlank()) {
            source = Objects.requireNonNullElse(
                ObjectAccessor.wrapObject(source)
                    .navigate(PropertyPath.forPath(annotation.prefix()))
                    .unwrap(),
                Map.of()
            );
        }
        var result = dataBinder.bind(source, parameter.getParameterType(), annotation.value());
        return BindingResultArgumentSupport.expose(result, parameter, context);
    }
}
