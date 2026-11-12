package org.jmouse.web.mvc.method.argument;

import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.mvc.Model;
import org.jmouse.web.mvc.RequestModel;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;

/** Resolves view state directly from request scope; invocation results are never used as a mutable context. */
public final class ModelArgumentResolver extends AbstractArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameterType() == Model.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, RequestContext context, MappingResult mapping) {
        return RequestModel.get(context.request());
    }
}
