package svit.web;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.matcher.Matcher;
import svit.reflection.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

import static svit.reflection.ClassMatchers.*;
import static svit.reflection.Reflections.getShortName;

/**
 * A {@link ServletContainerInitializer} implementation for initializing application-specific components.
 * Automatically detects and initializes implementations of {@link ApplicationInitializer} during the servlet container startup.
 */
@HandlesTypes(ApplicationInitializer.class)
public class FrameworkInitializer implements ServletContainerInitializer {

    /**
     * Logger for logging initialization process
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkInitializer.class);

    private final List<ApplicationInitializer> initializers = new ArrayList<>();

    public FrameworkInitializer(ApplicationInitializer... initializers) {
        this.initializers.addAll(List.of(initializers));
    }

    @Override
    public void onStartup(Set<Class<?>> initializerClasses, ServletContext servletContext) throws ServletException {
        Matcher<Class<?>> matcher = isSupertype(ApplicationInitializer.class)
                .and(isInterface().not()).and(isAbstract().not());

        if (initializerClasses != null) {
            for (Class<?> initializerClass : initializerClasses) {
                if (matcher.matches(initializerClass)) {

                    LOGGER.info("Applicable initializer: {}", getShortName(initializerClass));

                    try {
                        // Find the first constructor of the class
                        Constructor<?> constructor = Reflections.findFirstConstructor(initializerClass);

                        // Instantiate the class using the constructor
                        ApplicationInitializer initializer =
                                (ApplicationInitializer) Reflections.instantiate(constructor);

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

            // Invoke onStartup for each initializer
            for (ApplicationInitializer initializer : initializers) {
                String initializerName = getShortName(initializer.getClass());
                LOGGER.info("Before executing initializer: {}", initializerName);
                initializer.onStartup(servletContext);
                LOGGER.info("After executing initializer: {}", initializerName);
            }
        } else {
            servletContext.log("No ApplicationInitializers was found in classpath");
        }
    }

}
