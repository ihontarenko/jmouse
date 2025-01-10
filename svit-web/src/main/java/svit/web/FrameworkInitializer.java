package svit.web;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.HandlesTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.matcher.Matcher;
import svit.reflection.Reflections;
import svit.web.servlet.DispatcherServlet;
import svit.web.servlet.FrameworkDispatcherServlet;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * Scans for classes implementing {@link ApplicationInitializer}, instantiates them,
     * and invokes their {@link ApplicationInitializer#onStartup(ServletContext)} methods.
     *
     * @param initializerClasses the set of detected {@link ApplicationInitializer} classes.
     * @param servletContext     the {@link ServletContext} for the web application.
     * @throws ServletException if an error occurs during initialization.
     */
    @Override
    public void onStartup(Set<Class<?>> initializerClasses, ServletContext servletContext) throws ServletException {
        Set<ApplicationInitializer> initializers = new HashSet<>();
        Matcher<Class<?>>           matcher      = isSupertype(ApplicationInitializer.class).and(isInterface().not())
                .and(isAbstract().not());

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

        registerDispatcherServlet(servletContext);
    }

    protected void registerDispatcherServlet(ServletContext servletContext) {
        DispatcherServlet           dispatcher   = new FrameworkDispatcherServlet();
        ServletRegistration.Dynamic registration = servletContext.addServlet("FrameworkDispatcherServlet", dispatcher);
        registration.setLoadOnStartup(1);
        registration.addMapping("/*");
    }

}
