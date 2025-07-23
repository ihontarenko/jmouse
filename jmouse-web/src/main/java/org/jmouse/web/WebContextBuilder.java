package org.jmouse.web;

import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.web.context.WebBeanContext;

import java.util.function.Consumer;

public interface WebContextBuilder {
    WebContextBuilder name(String contextId);
    WebContextBuilder baseClasses(Class<?>... baseClasses);
    WebContextBuilder parent(WebBeanContext parent);
    WebContextBuilder addInitializer(BeanContextInitializer initializer);
    WebContextBuilder useDefault();
    WebContextBuilder useWebMvc();
    WebContextBuilder customize(Consumer<WebBeanContext> customizer);
    WebBeanContext build();
}
