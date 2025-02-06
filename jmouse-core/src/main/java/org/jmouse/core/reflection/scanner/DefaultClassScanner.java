package org.jmouse.core.reflection.scanner;

import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ReflectionException;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.Files;
import org.jmouse.util.Strings;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ClassScanner}.
 * <p>
 * This class provides functionality to scan the classpath for classes that match specific criteria,
 * leveraging Ant-style patterns for resource matching and supporting both JRT and regular classpath resources.
 * </p>
 */
public class DefaultClassScanner implements ClassScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClassScanner.class);

    private final PatternMatcherResourceLoader loader;

    /**
     * Constructs a {@link DefaultClassScanner} with the specified {@link PatternMatcherResourceLoader}.
     *
     * @param loader the resource loader to use for finding resources
     */
    public DefaultClassScanner(PatternMatcherResourceLoader loader) {
        this.loader = loader;
    }

    /**
     * Constructs a {@link DefaultClassScanner} with a default {@link CompositeResourceLoader}.
     */
    public DefaultClassScanner() {
        this(new CompositeResourceLoader());
    }

    /**
     * Scans the classpath for classes matching the given matcher and base classes.
     *
     * @param matcher     the matcher to filter classes
     * @param classLoader the class loader to use for loading classes
     * @param baseClasses the base classes or packages to scan
     * @return a set of classes matching the specified criteria
     */
    @Override
    public Set<Class<?>> scan(Matcher<Class<?>> matcher, ClassLoader classLoader, Class<?>... baseClasses) {
        Set<Class<?>> classes  = new HashSet<>();
        String        location = "classpath:%s";
        String        pattern  = "%s/**/*.class";

        for (Class<?> baseClass : baseClasses) {
            String  path    = Files.packageToPath(baseClass, Files.SLASH);
            String  antPath = pattern.formatted(location.formatted(path));

            // Adjust path for JRT resources
            if (Reflections.isJrtResource(baseClass)) {
                antPath = "%s:%s/%s".formatted(Resource.JRT_PROTOCOL, baseClass.getModule().getName(), path);
            }

            Collection<Resource> resources = loader.findResources(antPath);

            for (Resource resource : resources) {
                String className = Strings.extractClassName(baseClass, resource.getURL());
                try {
                    classes.add(Reflections.getClassFor(className));
                } catch (ReflectionException exception) {
                    LOGGER.trace("Unable to get class '{}'. Cause: {}", className, exception.getMessage());
                }
            }
        }

        return classes.stream().filter(matcher::matches).collect(Collectors.toSet());
    }



}
