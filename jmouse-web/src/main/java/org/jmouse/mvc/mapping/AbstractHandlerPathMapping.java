package org.jmouse.mvc.mapping;

import org.jmouse.mvc.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class AbstractHandlerPathMapping<H> extends AbstractHandlerMapping {

    private final Map<PathPattern, H>        handlers = new HashMap<>();
    private       HandlerInterceptorRegistry registry;

    public void addHandlerMapping(String route, H handler) {
        handlers.put(new PathPattern(route), handler);
    }

    public MappedHandler getMappedHandler(String path) {
        MappedHandler mappedHandler = null;

        for (Map.Entry<PathPattern, H> entry : handlers.entrySet()) {
            PathPattern pathPattern = entry.getKey();

            if (pathPattern.matches(path)) {
                RoutePath routePath = pathPattern.parse(path);
                mappedHandler = new MappedHandler(entry.getValue(), routePath);
            }
        }

        return mappedHandler;
    }

    @Override
    protected List<HandlerInterceptor> getHandlerInterceptors() {
        return registry.getInterceptors();
    }

    public void setHandlerInterceptorsRegistry(HandlerInterceptorRegistry registry) {
        this.registry = registry;
    }

}
