package org.jmouse.web.servlet;

import org.jmouse.web.request.HttpRequest;

public interface HandlerMapping {
    HandlerExecutionChain getHandler(HttpRequest request);
}
