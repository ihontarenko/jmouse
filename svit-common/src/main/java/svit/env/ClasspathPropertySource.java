package svit.env;

import svit.io.ClasspathResourceLoader;
import svit.io.Resource;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class ClasspathPropertySource extends JavaPropertiesPropertySource {

    private final ClasspathResourceLoader loader;
    private final String                  location;

    public ClasspathPropertySource(String name, String location) {
        super(name, null);
        this.location = location;
        this.loader = new ClasspathResourceLoader();
        loadProperties();
    }

    private void loadProperties() {
        Resource   resource   = loader.getResource(location);
        Properties properties = new Properties();

        try {
            properties.load(resource.getReader());
        } catch (IOException ignore) {
        }

        this.source = (Map) properties;
    }

}
