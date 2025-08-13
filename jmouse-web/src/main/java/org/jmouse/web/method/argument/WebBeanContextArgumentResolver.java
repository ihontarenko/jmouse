package org.jmouse.web.method.argument;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.MappingResult;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.method.AbstractArgumentResolver;
import org.jmouse.web.method.MethodParameter;

public class WebBeanContextArgumentResolver extends AbstractArgumentResolver implements BeanContextAware {

    private BeanContext beanContext;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && BeanContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            RequestContext requestContext,
            MappingResult mappingResult,
            InvocationOutcome invocationResult
    ) {
        return getBeanContext();
    }

    @Override
    public void setBeanContext(BeanContext context) {
        this.beanContext = context;
    }

    @Override
    public BeanContext getBeanContext() {
        return beanContext;
    }
}
