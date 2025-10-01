package org.jmouse.web.http.request;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.Scope;
import org.jmouse.web.context.WebContextException;


public interface RequestAttributes {

    String MVC_RESULT_ATTRIBUTE     = RequestAttributes.class.getName() + ".MVC_RESULT";
    String PRODUCES_TYPES_ATTRIBUTE = RequestAttributes.class.getName() + ".PRODUCES_TYPES";

    /**
     * Constant representing the request scope.
     */
    int REQUEST = 2;

    /**
     * Constant representing the session scope.
     */
    int SESSION = 3;

    /**
     * Retrieves an attribute by its name from the current scope (request or session).
     *
     * @param name the name of the attribute to retrieve
     * @return the value of the attribute, or {@code null} if not found
     */
    Object getAttribute(String name);

    /**
     * Sets an attribute in the current scope (request or session).
     *
     * @param name  the name of the attribute
     * @param value the value to set
     */
    void setAttribute(String name, Object value);

    /**
     * Removes an attribute by its name from the current scope (request or session).
     *
     * @param name the name of the attribute to remove
     */
    void removeAttribute(String name);

    /**
     * 🏗️ Factory method to create {@link RequestAttributes} for the given {@link Scope}.
     *
     * <p>Supported scopes:</p>
     * <ul>
     *   <li>📨 {@code REQUEST} → wraps into {@link WebHttpRequest}</li>
     *   <li>🗝️ {@code SESSION} → wraps into {@link WebHttpSession}</li>
     * </ul>
     *
     * @param scope   target scope (REQUEST or SESSION)
     * @param request current HTTP request
     * @return appropriate {@link RequestAttributes} implementation
     * @throws WebContextException if scope is unsupported
     */
    static RequestAttributes ofRequest(Scope scope, HttpServletRequest request) {
        return ofRequest(scope, request, false);
    }

    /**
     * 🏗️ Factory method to create {@link RequestAttributes} with session creation control.
     *
     * <p>Supported scopes:</p>
     * <ul>
     *   <li>📨 {@code REQUEST} → wraps into {@link WebHttpRequest}</li>
     *   <li>🗝️ {@code SESSION} → wraps into {@link WebHttpSession} (may allow session creation)</li>
     * </ul>
     *
     * @param scope                target scope (REQUEST or SESSION)
     * @param request              current HTTP request
     * @param allowSessionCreation whether creating new sessions is allowed
     * @return appropriate {@link RequestAttributes} implementation
     * @throws WebContextException if scope is unsupported
     */
    static RequestAttributes ofRequest(Scope scope, HttpServletRequest request, boolean allowSessionCreation) {
        return switch (scope.id()) {
            case REQUEST -> new WebHttpRequest(request);
            case SESSION -> new WebHttpSession(request, allowSessionCreation);
            default -> throw new WebContextException(
                    "Unsupported scope ID '%s' type. Available only REQUEST and SESSION".formatted(scope.id()));
        };
    }

}
