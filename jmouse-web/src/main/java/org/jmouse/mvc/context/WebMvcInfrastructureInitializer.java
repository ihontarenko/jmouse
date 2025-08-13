package org.jmouse.mvc.context;

import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.mvc.*;
import org.jmouse.web.ViewResolver;
import org.jmouse.web.method.ArgumentResolver;
import org.jmouse.web.ExceptionResolver;
import org.jmouse.web.method.converter.HttpMessageConverter;
import org.jmouse.web.method.converter.MessageConverterManager;
import org.jmouse.mvc.routing.MappingRegistry;

import java.util.ArrayList;

/**
 * 🛠 Initializes core Web MVC infrastructure components.
 *
 * <p>Scans the specified base packages for essential MVC components such as:
 * <ul>
 *   <li>{@link HandlerMapping}</li>
 *   <li>{@link ReturnValueHandler}</li>
 *   <li>{@link HttpMessageConverter}</li>
 *   <li>{@link MessageConverterManager}</li>
 *   <li>{@link HandlerAdapter}</li>
 *   <li>{@link ArgumentResolver}</li>
 *   <li>{@link ViewResolver}</li>
 *   <li>{@link ExceptionResolver}</li>
 *   <li>{@link MappingRegistry}</li>
 * </ul>
 *
 * <p>Registers found classes into the application context for later use.
 *
 * @author Ivan
 */
public class WebMvcInfrastructureInitializer extends ScannerBeanContextInitializer {

    /**
     * 📦 Creates a new initializer for the given base packages.
     *
     * @param basePackages packages to scan for MVC infrastructure components
     */
    public WebMvcInfrastructureInitializer(Class<?>... basePackages) {
        super(basePackages);

        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(HandlerMapping.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ReturnValueHandler.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(HttpMessageConverter.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findExactlyClasses(MessageConverterManager.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(HandlerAdapter.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ArgumentResolver.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ViewResolver.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findImplementations(ExceptionResolver.class, types)));
        addScanner(types -> new ArrayList<>(
                ClassFinder.findExactlyClasses(MappingRegistry.class, types)));
    }
}

