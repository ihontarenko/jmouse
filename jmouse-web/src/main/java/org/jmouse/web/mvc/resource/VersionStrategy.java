package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;

public interface VersionStrategy {

    PathVersion getVersion(String path);

    String removeVersion(String path, String version);

    String putVersion(PathVersion versionPath);

    PathVersion generateVersion(Resource resource);

    boolean isSupports(String requestPath);

}
