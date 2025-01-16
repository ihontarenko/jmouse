package svit.web.context.request;

import jakarta.servlet.http.HttpServletRequest;
import svit.beans.Scope;
import svit.web.context.WebContextException;


public interface RequestAttributes {

    /**
     * Constant representing the request scope.
     */
    int REQUEST = 0;

    /**
     * Constant representing the session scope.
     */
    int SESSION = 1;

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

    static RequestAttributes of(Scope scope, HttpServletRequest request) {
        return switch (scope.id()) {
            case REQUEST -> new ServletHttpRequest(request);
            case SESSION -> new ServletHttpSession(request);
            default -> throw new WebContextException(
                    "Unsupported scope type. Available only RequestAttributes#REQUEST and RequestAttributes@SESSION");
        };
    }
}
