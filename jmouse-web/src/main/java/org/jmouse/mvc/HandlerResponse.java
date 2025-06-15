package org.jmouse.mvc;

import org.jmouse.web.request.http.HttpStatus;

/**
 * 🧾 Represents the result returned from a {@link HandlerAdapter} after handling a request.
 * Used to encapsulate both the {@link HttpStatus} and the actual return value (e.g. model, view name, JSON, etc).
 *
 * @author Ivan Hontarenko
 * @see HandlerAdapter
 */
public interface HandlerResponse {

    /**
     * Returns the HTTP status that should be used for the response.
     *
     * @return an instance of {@link HttpStatus}; must not be {@code null}
     */
    HttpStatus getHttpStatus();

    /**
     * Returns the object that represents the result of request handling.
     * This can be a raw body value, a view name, or any structured data.
     *
     * @return the return value from the handler; may be {@code null}
     */
    Object getReturnValue();

}
