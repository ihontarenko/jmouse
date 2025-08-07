package org.jmouse.mvc;

public interface ExceptionResolver<T extends Throwable> {

    boolean supportsException(Throwable exception);

    void resolveException(RequestContext requestContext, InvocationOutcome outcome, T exception);

}
