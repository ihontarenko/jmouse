package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.mvc.BeanInstanceInitializer;
import org.jmouse.web.initializer.WebApplicationInitializer;

import java.util.ArrayList;

public class CoreScannerBeanContextInitializer extends ScannerBeanContextInitializer {

    public CoreScannerBeanContextInitializer(Class<?>... baseClasses) {
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
