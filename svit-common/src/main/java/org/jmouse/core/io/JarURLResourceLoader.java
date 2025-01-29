package org.jmouse.core.io;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.util.Files;
import org.jmouse.util.Jars;
import org.jmouse.util.JavaIO;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A {@link ResourceLoader} implementation for loading resources from JAR files.
 * <p>
 * This loader uses JAR-specific protocols and handles resource loading directly from JAR files.
 * It supports matching entries within the JAR and provides access to individual resources or collections of resources.
 * </p>
 */
public class JarURLResourceLoader extends AbstractResourceLoader {

    /**
     * Constructs a new {@link JarURLResourceLoader} with the specified class loader.
     *
     * @param classLoader the {@link ClassLoader} to use for loading resources
     */
    public JarURLResourceLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    /**
     * Constructs a new {@link JarURLResourceLoader} with the default class loader.
     */
    public JarURLResourceLoader() {
        this(JarURLResourceLoader.class.getClassLoader());
    }

    /**
     * Loads a single resource from the specified location.
     *
     * @param location the resource location
     * @return the loaded {@link Resource}
     */
    @Override
    public Resource getResource(String location) {
        return new JarURLResource(JavaIO.toURL(location, getClassLoader()));
    }

    /**
     * Loads resources from JAR files matching the specified location and matcher.
     *
     * @param location the base location within the JAR file
     * @param matcher  the matcher to filter resource names
     * @return a collection of {@link Resource} objects matching the criteria
     * @throws ResourceException if an I/O error occurs during resource loading
     */
    @Override
    public Collection<Resource> loadResources(String location, Matcher<String> matcher) {
        Collection<Resource> resources = new ArrayList<>();

        location = Files.removeProtocol(location);

        try {
            Enumeration<URL> enumeration = getClassLoader().getResources(location);

            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                if (supports(url.getProtocol())) {
                    resources.addAll(loadResources(location, url, matcher));
                }
            }

        } catch (IOException e) {
            throw new ResourceException("Failed to read resources from '%s' files".formatted(location), e);
        }

        return resources;
    }

    /**
     * Loads resources from a specific JAR file and URL.
     *
     * @param location the base location within the JAR file
     * @param jar      the {@link URL} of the JAR file
     * @param matcher  the matcher to filter resource names
     * @return a collection of {@link Resource} objects matching the criteria
     * @throws JarResourceException if an error occurs while reading the JAR file
     */
    public Collection<Resource> loadResources(String location, URL jar, Matcher<String> matcher) {
        Collection<Resource> resources = new ArrayList<>();

        try {
            JarURLConnection connection = (JarURLConnection) jar.openConnection();

            try (JarFile file = connection.getJarFile()) {

                Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (!entry.isDirectory()) {
                        String entryLocation = "%s%s%s".formatted(Jars.getBasePath(jar), Jars.JAR_TOKEN, entry.getName());
                        if (matcher.matches(entryLocation)) {
                            resources.add(getResource(entryLocation));
                        }
                    }
                }
            }
        } catch (Exception exception) {
            throw new JarResourceException("Failed to read jar resources from '%s' files".formatted(jar), exception);
        }

        return resources;
    }

    /**
     * Returns the list of supported protocols.
     *
     * @return a list containing "jar" as the supported protocol
     */
    @Override
    public List<String> supportedProtocols() {
        return List.of(Resource.JAR_PROTOCOL);
    }

}
