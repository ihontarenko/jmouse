package svit.reflection.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.io.*;
import svit.matcher.Matcher;
import svit.reflection.ReflectionException;
import svit.reflection.Reflections;
import svit.util.Files;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultClassScanner implements ClassScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClassScanner.class);

    private final PatternMatcherResourceLoader loader;

    public DefaultClassScanner(PatternMatcherResourceLoader loader) {
        this.loader = loader;
    }

    public DefaultClassScanner() {
        this(new CompositeResourceLoader());
    }

    @Override
    public Set<Class<?>> scan(Matcher<Class<?>> matcher, ClassLoader classLoader, Class<?>... baseClasses) {
        Set<Class<?>> classes  = new HashSet<>();
        String        location = "classpath:%s";
        String        pattern  = "%s/**/*.class";

        System.out.println(
                loader.getResource("classpath:package.txt").asString()
        );

        for (Class<?> baseClass : baseClasses) {
            String path    = Files.packageToPath(baseClass, Files.SLASH);
            boolean isJrt = Reflections.isJrtResource(baseClass);

            if (isJrt) {
                path = baseClass.getModule().getName();
            }

            String antPath = pattern.formatted(location.formatted(path));

            // replace protocol to 'jrt:'
            if (isJrt) {
                antPath = Resource.JRT_PROTOCOL + ":" + Files.removeProtocol(antPath);
            }

            Collection<Resource> resources = loader.findResources(antPath);

            for (Resource resource : resources) {
                String className = getClassName(baseClass, resource.getURL());
                try {
                    classes.add(Reflections.getClassFor(className));
                } catch (ReflectionException exception) {
                    LOGGER.error("Unable to get class '{}'. Cause: {}", className, exception.getMessage());
                }
            }
        }

        return classes.stream().filter(matcher::matches).collect(Collectors.toSet());
    }

    public String getClassName(Class<?> baseClass, URL url) {
        String basePath = Files.packageToPath(baseClass, Files.SLASH);
        String relative = Files.getRelativePath(url, basePath);
        return Files.removeExtension(relative).replace(Files.SLASH.charAt(0), '.');
    }

}
