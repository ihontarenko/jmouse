package org.jmouse.core.io;

import org.jmouse.util.helper.JavaIO;

import java.util.List;

import static org.jmouse.core.io.Resource.*;

/**
 * A {@link ResourceLoader} implementation for loading resources from network locations.
 * <p>
 * This loader supports HTTP and HTTPS protocols and uses {@link URLResource} for resource representation.
 * </p>
 */
public class NetworkURLResourceLoader extends AbstractResourceLoader {

    private final ClassLoader classLoader;

    /**
     * Constructs a new {@link NetworkURLResourceLoader} with the specified {@link ClassLoader}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading resources
     */
    public NetworkURLResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Constructs a new {@link NetworkURLResourceLoader} with the current thread's context {@link ClassLoader}.
     */
    public NetworkURLResourceLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads a single resource from the specified network location.
     *
     * @param location the resource location (must use HTTP or HTTPS protocol)
     * @return the loaded {@link Resource}
     * @throws ResourceLoaderException if the protocol is unsupported or the resource cannot be loaded
     */
    @Override
    public Resource getResource(String location) {
        ensureSupportedProtocol(location);
        return new URLResource(JavaIO.toURL(location, getClassLoader()));
    }

    /**
     * Returns the {@link ClassLoader} used by this loader.
     */
    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Returns the list of supported protocols.
     *
     * @return a list containing "http" and "https"
     */
    @Override
    public List<String> supportedProtocols() {
        return List.of(HTTP_PROTOCOL, HTTPS_PROTOCOL);
    }
}
