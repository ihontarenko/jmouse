package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.mvc.mapping.annnotation.Controller;
import org.jmouse.core.reflection.ClassFinder;

import java.util.ArrayList;

public class WebMvcControllersInitializer extends ScannerBeanContextInitializer {
    public WebMvcControllersInitializer(Class<?>... basePackages) {
        super(basePackages);

        addScanner(types -> new ArrayList<>(
                ClassFinder.findAnnotatedClasses(Controller.class, types)));
    }
}
