package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.mvc.BeanInstanceInitializer;
import org.jmouse.util.Priority;
import org.jmouse.web.initializer.WebApplicationInitializer;

import java.util.ArrayList;

@Priority(50)
public class WebMvcApplicationScannerBeanContextInitializer extends ScannerBeanContextInitializer {

    public WebMvcApplicationScannerBeanContextInitializer(Class<?>... baseClasses) {
        super(baseClasses);

        addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findAnnotatedClasses(BeanProperties.class, rootTypes)));
        addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(BeanInstanceInitializer.class, rootTypes)));
        addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(ApplicationConfigurer.class, rootTypes)));
        addScanner(rootTypes -> new ArrayList<>(
                ClassFinder.findImplementations(WebApplicationInitializer.class, rootTypes)));
    }
}
