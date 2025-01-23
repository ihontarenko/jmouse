package svit.io;

import java.net.URL;

public class JarURLResource extends URLResource {

    public JarURLResource(URL url) {
        super(url);
    }

    /**
     * Returns a human-readable name for the resource.
     */
    @Override
    public String getResourceName() {
        return "JAR:%s".formatted(super.getResourceName());
    }

}
