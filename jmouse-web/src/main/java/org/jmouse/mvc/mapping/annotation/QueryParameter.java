package org.jmouse.mvc.mapping.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 🔎 Annotation for mapping a route based on a query parameter and its expected value.
 *
 * <p>Useful when you want to restrict route handling to requests with specific query parameters.
 *
 * <pre>{@code
 * @QueryParameter(name = "lang", value = "uk")
 * public void handleUkrainianLang() {
 *     // will be triggered only if ?lang=uk
 * }
 * }</pre>
 *
 * 🧩 Can be used alongside other conditions like {@link Header}, etc.
 *
 * @see org.jmouse.web.request.RequestRoute
 * @see org.jmouse.mvc.Route
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParameter {

    /**
     * 🌐 Name of the query parameter (e.g. "lang", "version").
     */
    String name();

    /**
     * 🎯 Expected value of the parameter (e.g. "uk", "v2").
     */
    String value();

}
