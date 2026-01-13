package org.jmouse.common;

import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceLoader;

public class ResourceReader {

    public static String readFileToString(String path) {
        ResourceLoader resourceLoader = new CompositeResourceLoader();
        Resource       resource       = resourceLoader.getResource(path);
        return asString(resource);
    }

    public static String asString(Resource resource) {
        return resource.asString();
    }

}
