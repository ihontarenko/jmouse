package svit.io;

import svit.reflection.Reflections;
import svit.util.Files;

import static svit.reflection.Reflections.getShortName;
import static svit.util.Files.extractProtocol;

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
        if (!supports(extractProtocol(location, null))) {
            throw new ResourceLoaderException(
                    "Protocol '%s' not supported by '%s' loader. Available: %s"
                            .formatted(extractProtocol(location, "undefined"), getShortName(this), supportedProtocols()));
        }
    }


}
