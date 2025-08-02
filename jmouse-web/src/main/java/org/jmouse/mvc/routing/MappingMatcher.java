
package org.jmouse.mvc.routing;

import org.jmouse.web.request.RequestRoute;

public interface MappingMatcher {

    boolean matches(RequestRoute requestRoute);

    int compareWith(MappingMatcher other, RequestRoute requestRoute);

}
