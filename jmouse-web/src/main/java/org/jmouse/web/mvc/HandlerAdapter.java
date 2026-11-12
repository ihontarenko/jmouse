package org.jmouse.web.mvc;

import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.result.HandlerResult;

/** Invokes one supported handler kind. Response rendering belongs to the dispatcher. */
public interface HandlerAdapter {
    boolean supportsHandler(MappedHandler handler);

    /** Returns an explicit invocation outcome; implementations must never use null for handled responses. */
    HandlerResult handle(RequestContext context, MappedHandler handler);
}
