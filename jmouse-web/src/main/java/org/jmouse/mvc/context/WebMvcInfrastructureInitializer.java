package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.mvc.HandlerAdapter;
import org.jmouse.mvc.HandlerMapping;
import org.jmouse.mvc.ReturnValueHandler;

import java.util.ArrayList;

public class WebMvcInfrastructureInitializer extends ScannerBeanContextInitializer {
    public WebMvcInfrastructureInitializer(Class<?>... basePackages) {
        super(basePackages);

        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(HandlerMapping.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ReturnValueHandler.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(HandlerAdapter.class, types)));
    }
}
