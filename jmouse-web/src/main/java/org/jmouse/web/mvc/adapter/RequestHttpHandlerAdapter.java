package org.jmouse.web.mvc.adapter;

import java.io.IOException;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.HandlerAdapter;
import org.jmouse.web.mvc.HandlerAdapterException;
import org.jmouse.web.mvc.MappedHandler;
import org.jmouse.web.mvc.result.HandlerResult;

/** Invokes streaming/static/direct handlers and explicitly marks the response as handled. */
public final class RequestHttpHandlerAdapter implements HandlerAdapter {

    @Override
    public HandlerResult handle(RequestContext context, MappedHandler handler) {
        try {
            ((RequestHttpHandler) handler.handler()).handle(context.request(), context.response());
            return HandlerResult.handled();
        } catch (IOException failure) {
            throw new HandlerAdapterException("HTTP handler execution failed", failure);
        }
    }

    @Override
    public boolean supportsHandler(MappedHandler handler) {
        return handler.handler() instanceof RequestHttpHandler;
    }
}
