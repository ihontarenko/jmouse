package org.jmouse.mvc;

import org.jmouse.context.ApplicationFactory;
import org.jmouse.mvc.context.WebControllersInitializer;
import org.jmouse.mvc.context.WebInfrastructureInitializer;
import org.jmouse.web.context.WebBeanContext;

public class ServletContextManager {

    private final WebBeanContext rootContext;

    public ServletContextManager(WebBeanContext rootContext) {
        this.rootContext = rootContext;
    }

    @SuppressWarnings("unchecked")
    public WebBeanContext createDispatcherContext(String name, Class<?>[] basePackages) {
        ApplicationFactory<WebBeanContext> factory           = rootContext.getBean(ApplicationFactory.class);
        WebBeanContext                     dispatcherContext = factory.createContext(name, rootContext);

        dispatcherContext.addInitializer(new WebInfrastructureInitializer(basePackages));
        dispatcherContext.addInitializer(new WebControllersInitializer(basePackages));

        dispatcherContext.refresh();

        return dispatcherContext;
    }

}
