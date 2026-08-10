package org.jmouse.access.enforcement;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * What the outside world calls a method's parameters — the one thing enforcement cannot work out on
 * its own.
 *
 * <p>{@code @RequiresAccess(resource = Entry.class, resourceId = "entryId")} has to find the argument
 * the caller actually passed. The name it matches on is the one the <em>request</em> uses, and only
 * the framework serving the request knows that: in Spring MVC it is the value in
 * {@code @PathVariable("entryId")}, in another stack it is something else, and in a plain method call
 * it is simply the parameter's declared name.
 *
 * <p>So enforcement asks rather than assumes. {@link #declared()} is the honest default for a caller
 * with no web framework at all; a Spring adapter answers from {@code @PathVariable} and
 * {@code @RequestParam}, and a jMouse-web one from whatever it uses.
 *
 * <p>⚠️ {@link #declared()} depends on the class being compiled with {@code -parameters}. Without it
 * the JDK reports {@code arg0}, {@code arg1} and a declaration naming {@code entryId} silently matches
 * nothing — which is why an adapter that knows the real names is worth having.
 */
public interface ParameterNaming {

    /** Parameter names as the compiler recorded them, and identifiers left unmarked. */
    static ParameterNaming declared() {
        return new ParameterNaming() {

            @Override
            public Optional<String> requestName(Parameter parameter) {
                return Optional.empty();
            }

            @Override
            public boolean isIdentifier(Parameter parameter) {
                return false;
            }
        };
    }

    /**
     * What the request calls this parameter, or empty to fall back to its declared name.
     */
    Optional<String> requestName(Parameter parameter);

    /**
     * Whether this parameter carries a row identifier out of the route.
     *
     * <p>Read by {@code resourceId}'s default: a method with exactly one of these needs no attribute
     * at all. A method with two gets a startup failure rather than a guess — choosing which identifier
     * a route is about is how a check comes to be made about the wrong row.
     */
    boolean isIdentifier(Parameter parameter);
}
