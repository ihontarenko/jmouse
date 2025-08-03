package org.jmouse.mvc.mapping.annnotation;

/**
 * 📬 Annotation for mapping a route based on a specific HTTP header and its value.
 *
 * <p>Used to declare that a handler method or class should be invoked only if a request contains the given header.
 *
 * <pre>{@code
 * @Header(name = "X-Requested-With", value = "XMLHttpRequest")
 * public void handleAjaxOnly() {
 *     // logic for AJAX requests
 * }
 * }</pre>
 *
 * 🧩 Often used in combination with other mapping annotations.
 *
 * @see org.jmouse.web.request.http.HttpHeader
 * @see org.jmouse.mvc.Route
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public @interface Header {

    /**
     * 📌 Header name (e.g., "Content-Type", "Authorization").
     */
    String name();

    /**
     * 📌 Expected value of the header (exact match).
     */
    String value();

}
