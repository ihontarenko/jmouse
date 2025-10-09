package org.jmouse.web.mvc.resource;

import org.jmouse.core.io.Resource;
import org.jmouse.web.http.Headers;

public interface HttpResource extends Resource {

    Headers getHeaders();

}
