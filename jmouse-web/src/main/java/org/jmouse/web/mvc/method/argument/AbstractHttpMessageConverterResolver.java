package org.jmouse.web.mvc.method.argument;

import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;
import org.jmouse.web.mvc.method.converter.MessageConverterManager;

abstract public class AbstractHttpMessageConverterResolver extends AbstractArgumentResolver {

    private MessageConverterManager converterManager;

    @Override
    public void doInitialize(WebBeanContext context) {
        converterManager = context.getBean(MessageConverterManager.class);
    }

    public MessageConverterManager getConverterManager() {
        return converterManager;
    }

}
