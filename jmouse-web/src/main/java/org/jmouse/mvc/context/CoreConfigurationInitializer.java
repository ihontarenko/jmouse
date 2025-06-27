package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.reflection.ClassFinder;

import java.util.ArrayList;

public class CoreConfigurationInitializer extends ScannerBeanContextInitializer {

    public CoreConfigurationInitializer(Class<?>... basePackages) {
        super(basePackages);

        addScanner(types -> new ArrayList<>(
                ClassFinder.findAnnotatedClasses(BeanProperties.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ApplicationConfigurer.class, types)));
    }

}
