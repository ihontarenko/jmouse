package svit.io;

import svit.util.JavaIO;

import java.util.List;

import static svit.io.Resource.*;

public class NetworkURLResourceLoader extends AbstractResourceLoader {

    private final ClassLoader classLoader;

    public NetworkURLResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public NetworkURLResourceLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Resource getResource(String location) {
        ensureSupportedProtocol(location);
        return new URLResource(JavaIO.toURL(location, getClassLoader()));
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public List<String> supportedProtocols() {
        return List.of(HTTP_PROTOCOL, HTTPS_PROTOCOL);
    }

}
