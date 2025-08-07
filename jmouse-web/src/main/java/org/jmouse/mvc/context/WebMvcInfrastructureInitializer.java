package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.mvc.*;
import org.jmouse.mvc.routing.MappingRegistry;

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
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ArgumentResolver.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ViewResolver.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ExceptionResolver.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findInheritedClasses(MappingRegistry.class, types)));
    }
}
