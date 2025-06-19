package org.jmouse.web.servlet.initializer;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Sorter;
import org.jmouse.web.initializer.WebApplicationInitializer;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.jmouse.core.reflection.ClassMatchers.*;
import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * A {@link ServletContainerInitializer} implementation for initializing application-specific components.
 * Automatically detects and initializes implementations of {@link WebApplicationInitializer} during the servlet container startup.
 */
@HandlesTypes(WebApplicationInitializer.class)
public class jMouseServletContainerInitializer implements ServletContainerInitializer {

    /**
     * Logger for logging initialization process
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(jMouseServletContainerInitializer.class);

    private final List<WebApplicationInitializer> initializers = new ArrayList<>();

    public jMouseServletContainerInitializer(WebApplicationInitializer... initializers) {
        this.initializers.addAll(List.of(initializers));
    }

    @Override
    public void onStartup(Set<Class<?>> initializerClasses, ServletContext servletContext) throws ServletException {
        Matcher<Class<?>> matcher = isSupertype(WebApplicationInitializer.class)
                .and(isInterface().not()).and(isAbstract().not());

        if (initializerClasses != null) {
            for (Class<?> initializerClass : initializerClasses) {
                if (matcher.matches(initializerClass)) {

                    LOGGER.info("Applicable initializer: {}", getShortName(initializerClass));

                    try {
                        // Find the first constructor of the class
                        Constructor<?> constructor = Reflections.findFirstConstructor(initializerClass);

                        // Instantiate the class using the constructor
                        WebApplicationInitializer initializer =
                                (WebApplicationInitializer) Reflections.instantiate(constructor);

                        initializers.add(initializer);
                    } catch (Throwable throwable) {
                        throw new ServletException(
                                "Couldn't instantiate initializer: %s".formatted(initializerClass), throwable);
                    }
                }
            }
        }

        executeInitializers(servletContext);
    }

    protected void executeInitializers(ServletContext servletContext) throws ServletException {
        if (!initializers.isEmpty()) {
            servletContext.log("ApplicationInitializer (%d) was found in classpath".formatted(initializers.size()));

            LOGGER.info("Start executing initializers");

            initializers.sort(Sorter.PRIORITY_COMPARATOR);

            LOGGER.info("Initializers {} before executing", initializers.size());

            // Invoke onStartup for each initializer
            for (WebApplicationInitializer initializer : initializers) {
                String initializerName = getShortName(initializer.getClass());
                LOGGER.info("Start executing initializer: {}", initializerName);
                initializer.onStartup(servletContext);
                LOGGER.info("Finish executing initializer: {}", initializerName);
            }

            LOGGER.info("Initializers {} executed successfully", initializers.size());
        } else {
            servletContext.log("No ApplicationInitializers was found in classpath");
        }
    }

}
