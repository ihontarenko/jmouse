package org.jmouse.core.io;

import static org.jmouse.core.reflection.Reflections.getShortName;
import static org.jmouse.util.Files.extractProtocol;

abstract public class AbstractResourceLoader implements ResourceLoader {

    private final ClassLoader classLoader;

    public AbstractResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public AbstractResourceLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Retrieves the {@link ClassLoader} used for loading resources.
     */
    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void ensureSupportedProtocol(String location) {
        if (!supports(extractProtocol(location, Resource.UNKNOWN_PROTOCOL))) {
            throw new ResourceLoaderException(
                    "Protocol '%s' not supported by '%s' loader. Available: %s"
                            .formatted(extractProtocol(location, "undefined"), getShortName(this), supportedProtocols()));
        }
    }


}
