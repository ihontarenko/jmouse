package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.mvc.BeanInstanceInitializer;
import org.jmouse.web.server.WebServerFactory;

import java.util.ArrayList;

public class CoreFrameworkInitializer extends ScannerBeanContextInitializer {

    public CoreFrameworkInitializer(Class<?>... basePackages) {
        super(basePackages);

        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(WebServerFactory.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(BeanInstanceInitializer.class, types)));
    }

}
