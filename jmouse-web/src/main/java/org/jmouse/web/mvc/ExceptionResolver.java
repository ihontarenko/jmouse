package org.jmouse.web.mvc;

import java.util.Optional;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.result.HandlerResult;

/** Resolves a supported execution failure; empty means another resolver may try. */
public interface ExceptionResolver {
    boolean supportsException(Throwable exception);

    /** Returns an explicit response outcome or an explicit decline. Never returns null. */
    Optional<HandlerResult> resolveException(
        RequestContext context,
        MappedHandler handler,
        Exception failure
    );
}
