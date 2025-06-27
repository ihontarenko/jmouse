package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.context.Controller;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.web.initializer.WebApplicationInitializer;

import java.util.ArrayList;

public class WebControllersInitializer extends ScannerBeanContextInitializer {
    public WebControllersInitializer(Class<?>... basePackages) {
        super(basePackages);

        addScanner(types -> new ArrayList<>(
                ClassFinder.findAnnotatedClasses(Controller.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(WebApplicationInitializer.class, types)));
    }
}
