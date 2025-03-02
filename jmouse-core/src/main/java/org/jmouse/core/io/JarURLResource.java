package org.jmouse.core.io;

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

}
