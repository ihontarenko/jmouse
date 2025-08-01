package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.AbstractHandlerAdapter;
import org.jmouse.mvc.HandlerAdapterException;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.ExecutionResult;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpStatus;

import java.io.IOException;

public class FunctionalRouteAdapter extends AbstractHandlerAdapter {

    @Override
    protected Object doHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler, ExecutionResult executionResult) {
        FunctionalRoute functionalRoute;

        if (handler instanceof MappedHandler mappedHandler) {
            functionalRoute = (FunctionalRoute) mappedHandler.handler();
        } else {
            functionalRoute = (FunctionalRoute) handler;
        }

        try {
            functionalRoute.handle(request, response);
            postHandle(response, executionResult);
            response.flushBuffer();
        } catch (IOException e) {
            executionResult.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            throw new HandlerAdapterException("Handler failed during execution.", e);
        }

        return null;
    }

    private void postHandle(HttpServletResponse response, ExecutionResult result) {
        String  contentType = response.getContentType();
        Headers headers     = result.getHeaders();

        if (contentType == null || contentType.isBlank()) {
            headers.setContentType(MediaType.TEXT_HTML);
            response.setContentType(headers.getContentType().toString());
        }

        if (response.getStatus() != 0) {
            result.setHttpStatus(HttpStatus.ofCode(response.getStatus()));
        }
    }

    @Override
    public boolean supportsHandler(Object handler) {
        return handler instanceof FunctionalRoute;
    }

}
