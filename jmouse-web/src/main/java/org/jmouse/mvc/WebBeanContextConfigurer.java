package org.jmouse.mvc;

import org.jmouse.beans.BeanScanAnnotatedContextInitializer;
import org.jmouse.context.ApplicationContextBeansScanner;
import org.jmouse.mvc.context.WebMvcControllersInitializer;
import org.jmouse.mvc.context.WebMvcInfrastructureInitializer;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.context.StartupRootApplicationContextInitializer;

public class WebBeanContextConfigurer {

    public void defaultInitializers(WebBeanContext rootContext) {
        rootContext.addInitializer(new BeanScanAnnotatedContextInitializer());
        rootContext.addInitializer(new StartupRootApplicationContextInitializer(rootContext.getEnvironment()));
        rootContext.addInitializer(new ApplicationContextBeansScanner());
    }

    public void webmvcInitializers(WebBeanContext rootContext) {
        rootContext.addInitializer(new WebMvcControllersInitializer());
        rootContext.addInitializer(new WebMvcInfrastructureInitializer());
    }

}
