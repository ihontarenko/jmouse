package org.jmouse.web.mvc.method.argument;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.core.MethodParameter;

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
            MappingResult mappingResult
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
