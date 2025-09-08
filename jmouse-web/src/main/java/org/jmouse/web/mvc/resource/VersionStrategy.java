package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;

public interface VersionStrategy {

    VersionPath getVersion(String path);

    String removeVersion(String path, String version);

    String putVersion(VersionPath versionPath);

    VersionPath generateVersion(Resource resource);

    boolean isSupports(String requestPath);

}
