package org.jmouse.web.mvc.method.argument;

import org.jmouse.beans.BeanContext;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.web.mvc.method.converter.MessageConverterManager;

abstract public class AbstractHttpMessageConverterResolver extends AbstractArgumentResolver {

    protected MessageConverterManager manager;
    protected BeanContext             context;

    @Override
    public void doInitialize(WebBeanContext context) {
        this.context = context;
        this.manager = context.getBean(MessageConverterManager.class);
    }

    public MessageConverterManager getConverterManager() {
        return manager;
    }

}
