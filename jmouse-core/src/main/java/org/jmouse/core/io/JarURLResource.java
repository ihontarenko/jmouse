package org.jmouse.core.io;

import java.io.File;
import java.net.URI;
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
        return "JAR_%s".formatted(super.getResourceName());
    }

    @Override
    public long getLastModified() {
        try {
            String path      = getURL().getPath();
            int    separator = path.indexOf("!/");

            if (separator != -1) {
                String part = path.substring(0, separator);
                URL    file = new URI(part).toURL();
                return new File(file.toURI()).lastModified();
            }

            return super.getLastModified();
        } catch (Exception e) {
            return 0;
        }
    }

}
