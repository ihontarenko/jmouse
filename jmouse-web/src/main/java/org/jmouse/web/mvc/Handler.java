package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jmouse.web.mvc.result.HandlerResult;

/** A mapped handler and its ordered execution interceptors; invocation and rendering belong to the dispatcher. */
public final class Handler {

    private final List<HandlerInterceptor> interceptors = new ArrayList<>();
    private MappedHandler handler;

    public Handler(MappedHandler handler) {
        setHandler(handler);
    }

    public List<HandlerInterceptor> getInterceptors() {
        return List.copyOf(interceptors);
    }

    public void addInterceptor(HandlerInterceptor interceptor) {
        interceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
    }

    public void addInterceptor(int position, HandlerInterceptor interceptor) {
        interceptors.add(position, Objects.requireNonNull(interceptor, "interceptor"));
    }

    public MappedHandler getHandler() {
        return handler;
    }

    public void setHandler(MappedHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        for (HandlerInterceptor interceptor : getInterceptors()) {
            if (!interceptor.preHandle(request, response, handler)) return false;
        }
        return true;
    }

    public HandlerResult postHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        HandlerResult result
    ) {
        HandlerResult current = Objects.requireNonNull(result, "result");
        for (HandlerInterceptor interceptor : getInterceptors()) {
            current = Objects.requireNonNull(
                interceptor.postHandle(request, response, handler, current),
                "HandlerInterceptor must return an explicit outcome"
            );
        }
        return current;
    }
}
