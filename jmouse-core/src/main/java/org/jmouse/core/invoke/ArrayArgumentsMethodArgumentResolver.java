package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;

import java.util.Arrays;
import java.util.List;

/**
 * {@link MethodArgumentResolver} that resolves arguments from a predefined array/list. 📦
 *
 * <p>
 * Arguments are matched by the parameter index of the invoked method.
 * This resolver typically acts as a simple positional argument provider.
 * </p>
 */
public class ArrayArgumentsMethodArgumentResolver implements MethodArgumentResolver {

    private final List<Object> arguments;

    /**
     * Creates resolver from vararg arguments.
     *
     * <h2>⚠️ {@link Arrays#asList} rather than {@link List#of}, and it is not a style choice</h2>
     *
     * <p>{@code List.of} rejects a null element with a bare {@link NullPointerException}. These are
     * <strong>call arguments</strong>, and a null one is ordinary — an optional parameter left out, an
     * expression reading a value nobody has filled in yet. Refusing it here fails the whole call
     * <em>before anything has looked at the method</em>, so the exception names no argument, no
     * position and no method, and reads exactly like a broken target.</p>
     *
     * <p>It cost hours once: an expression cascade — <em>the parts of the type chosen next door</em> —
     * broke precisely while its parent field was still empty, which is the state such a control always
     * opens in. That looks like a feature that never worked rather than one unhandled case.</p>
     *
     * <p>{@code Arrays.asList} is still fixed-size, so nothing downstream gains the ability to add or
     * remove an argument; it simply permits a null to travel to the method that was asked for. A method
     * that will not accept one then refuses in its own words, which is the right refusal.</p>
     *
     * @param arguments argument values, null elements included
     */
    public ArrayArgumentsMethodArgumentResolver(Object... arguments) {
        this(Arrays.asList(arguments));
    }

    /**
     * Creates resolver from argument list.
     *
     * <p>⚠️ The list is taken as given. A caller that built an immutable one owns that decision —
     * copying it here to permit nulls would silently change the meaning of somebody else's collection.
     *
     * @param arguments argument values
     */
    public ArrayArgumentsMethodArgumentResolver(List<Object> arguments) {
        this.arguments = arguments;
    }

    /**
     * Supports all parameters.
     */
    @Override
    public boolean supports(MethodParameter parameter) {
        return true;
    }

    /**
     * Returns argument by parameter index.
     */
    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        return arguments.get(parameter.getParameterIndex());
    }

}