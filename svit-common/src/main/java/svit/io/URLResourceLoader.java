package svit.io;

import java.util.List;

import static svit.io.Resource.*;

public class URLResourceLoader extends AbstractResourceLoader {

    private final ClassLoader classLoader;

    public URLResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public URLResourceLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Resource getResource(String location) {
        ensureSupportedProtocol(location);

        return new URLResource(getClassLoader().getResource(location));
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public List<String> supportedProtocols() {
        return List.of(HTTP_URL, HTTPS_URL, FILE_URL);
    }

}
