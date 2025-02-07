package org.jmouse.core.io;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.util.helper.Files;
import org.jmouse.util.helper.Jars;
import org.jmouse.util.helper.JavaIO;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * A {@link ResourceLoader} implementation for loading resources from the classpath.
 * <p>
 * This loader handles both JAR-based and file-based resources, delegating resource loading
 * to specialized loaders like {@link JarURLResourceLoader} and {@link FileSystemResourceLoader}.
 * </p>
 */
public class ClasspathResourceLoader extends AbstractResourceLoader {

    /**
     * Loads resources from the classpath matching the specified location and matcher.
     *
     * @param location the resource location in the classpath
     * @param matcher  the matcher to filter resource names
     * @return a collection of {@link Resource} objects matching the criteria
     * @throws ResourceException if an error occurs during resource loading
     */
    @Override
    public Collection<Resource> loadResources(String location, Matcher<String> matcher) {
        Collection<Resource> resources = new ArrayList<>();

        ensureSupportedProtocol(location);

        location = Files.removeProtocol(location);

        try {
            Enumeration<URL> enumeration = getClassLoader().getResources(location);

            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();

                switch (url.getProtocol()) {
                    case Resource.JAR_PROTOCOL:
                        resources.addAll(new JarURLResourceLoader(getClassLoader())
                                                 .loadResources(location, url, matcher));
                        break;
                    case Resource.FILE_PROTOCOL:
                        resources.addAll(new FileSystemResourceLoader()
                                                 .loadResources(location, Paths.get(url.toURI()), matcher));
                        break;
                    default:
                        throw new ResourceLoaderException("Protocol '%s' not supported.".formatted(url.getProtocol()));
                }

            }

        } catch (Exception e) {
            throw new ResourceException("Failed to read resources from '%s' files".formatted(location), e);
        }

        return resources;
    }

    /**
     * Loads a single resource from the classpath.
     *
     * @param location the resource location
     * @return the loaded {@link Resource}
     * @throws ResourceException if the resource cannot be resolved
     */
    @Override
    public Resource getResource(String location) {
        URL url = JavaIO.getURLResource(Files.removeProtocol(location), getClassLoader());

        if (url == null) {
            throw new ResourceLoaderException("Failed to find resource '%s'".formatted(location));
        }

        return Jars.isJarURL(location) ? new JarURLResource(url) : new URLResource(url);
    }

    /**
     * Returns the list of supported protocols.
     *
     * @return a list containing "classpath"
     */
    @Override
    public List<String> supportedProtocols() {
        return List.of(Resource.CLASSPATH_PROTOCOL);
    }

}
