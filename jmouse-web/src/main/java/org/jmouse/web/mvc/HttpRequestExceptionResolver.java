package org.jmouse.web.mvc;

import java.util.Optional;
import org.jmouse.core.Priority;
import org.jmouse.http.HttpStatus;
import org.jmouse.web.binding.BindingException;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.converter.UnreadableException;
import org.jmouse.web.http.converter.UnsuitableException;
import org.jmouse.web.mvc.result.HandlerResult;

/** Safe final fallback; application exception handlers may supply their own response bodies. */
@Priority(Integer.MAX_VALUE)
public final class HttpRequestExceptionResolver implements ExceptionResolver {

    @Override
    public boolean supportsException(Throwable exception) {
        return (
            exception instanceof HttpRequestException ||
            exception instanceof BindingException ||
            exception instanceof UnreadableException ||
            exception instanceof UnsuitableException
        );
    }

    @Override
    public Optional<HandlerResult> resolveException(
        RequestContext context,
        MappedHandler handler,
        Exception exception
    ) {
        HttpStatus status =
            exception instanceof HttpRequestException requestFailure
                ? requestFailure.status()
                : exception instanceof UnsuitableException
                  ? HttpStatus.NOT_ACCEPTABLE
                  : HttpStatus.BAD_REQUEST;
        context.response().setStatus(status.getCode());
        return Optional.of(HandlerResult.handled());
    }
}
