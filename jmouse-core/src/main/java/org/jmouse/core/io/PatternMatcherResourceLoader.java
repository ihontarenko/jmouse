package org.jmouse.core.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.matcher.ant.AntMatcher;
import org.jmouse.util.Files;
import org.jmouse.util.Strings;

import java.util.Collection;

import static org.jmouse.core.matcher.TextMatchers.ant;

/**
 * Interface for resource loaders that support pattern matching.
 * <p>
 * Provides functionality for finding resources based on Ant-style patterns, such as {@code '✶✶/✶.txt'}.
 * This interface extends {@link ResourceLoader} and adds pattern-based search capabilities.
 * </p>
 * Example usage:
 * <pre>{@code
 * PatternMatcherResourceLoader loader = new ClasspathResourceLoader();
 * Collection<Resource> resources = loader.findResources("classpath:/resources/✶✶/✶.txt");
 * resources.forEach(resource -> System.out.println(resource.getName()));
 * }</pre>
 */
public interface PatternMatcherResourceLoader extends ResourceLoader {

    Logger LOGGER = LoggerFactory.getLogger(PatternMatcherResourceLoader.class);

    /**
     * Finds resources that match the specified Ant-style pattern.
     *
     * @param path the path with an Ant-style pattern (e.g., {@code "classpath:✶✶/✶.txt"})
     * @return a collection of {@link Resource} objects matching the pattern
     * @throws ResourceLoaderException if an error occurs during resource loading
     */
    default Collection<Resource> findResources(String path) {
        String          protocol = Files.extractProtocol(path, Resource.CLASSPATH_PROTOCOL);
        String          location = Strings.prefix(path, AntMatcher.ANY_SINGLE_SEGMENT, false);
        String          pattern  = Files.removeProtocol(path);
        Matcher<String> matcher  = ant("**/" + pattern);

        LOGGER.info("Protocol: {}, Location: {}, {}", protocol, location, matcher);
        Collection<Resource> resources = loadResources(location, matcher);
        LOGGER.info("Found {} resources", resources.size());

        return resources;
    }

}
