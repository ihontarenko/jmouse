package org.jmouse.query.el.function;

import org.jmouse.query.el.QueryParseException;

import java.util.Collection;
import java.util.List;

/**
 * A call that cannot be resolved into the function it names.
 *
 * <p>⚠️ Every message names the fix, because a function library is written by somebody composing a
 * filter rather than reading a stack trace — and a call that failed is the moment they most need to be
 * told which name, which parameter, which chain.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FunctionCallException extends QueryParseException {

    public FunctionCallException(String message) {
        super(message);
    }

    /**
     * No function is declared under that name.
     *
     * <p>⚠️ Named as a <em>function</em> rather than left to be mistaken for a data-source function. A
     * document calling {@code low_stok(3)} must not be told "SQL has no such function" — it has to be
     * told the document does not declare one, and which ones it does.</p>
     */
    public static FunctionCallException unknown(String name, Collection<String> declared) {
        if (declared.isEmpty()) {
            return new FunctionCallException(
                    "there is no function called '%s' — this document declares none".formatted(name));
        }

        return new FunctionCallException(
                "there is no function called '%s'; this document declares %s".formatted(
                        name, String.join(", ", declared)));
    }

    /** Too many arguments for the parameters there are. */
    public static FunctionCallException tooManyArguments(String name, int given, int declared) {
        return new FunctionCallException(
                "'%s' takes %d argument(s) and was given %d".formatted(name, declared, given));
    }

    /** A parameter with no argument and no default. */
    public static FunctionCallException missingArgument(String name, String parameter) {
        return new FunctionCallException(
                "'%s' needs a value for '%s', and that parameter has no default".formatted(name, parameter));
    }

    /**
     * A function that calls itself, directly or through others.
     *
     * <p>⚠️ Refused rather than bounded, and it is part of the definition. jMQ is <strong>total</strong> —
     * every expression terminates — which is exactly what keeps a sandbox with timeouts and memory limits
     * out of the design. One recursive function breaks that property for the whole language, so the cycle
     * is named and refused.</p>
     */
    public static FunctionCallException recursive(List<String> chain) {
        return new FunctionCallException(
                ("'%s' calls itself (%s), and a query has to terminate — "
                 + "write the condition out rather than recursing")
                        .formatted(chain.getFirst(), String.join(" → ", chain)));
    }

    /** An argument that cannot be what the parameter was declared to hold. */
    public static FunctionCallException wrongType(String name, String parameter, String declared, String given) {
        return new FunctionCallException(
                "'%s' expects '%s' to be %s, and was given %s".formatted(name, parameter, declared, given));
    }

    /** A function whose body says nothing to inline. */
    public static FunctionCallException nothingToInline(String name) {
        return new FunctionCallException(
                "'%s' has no 'where' clause, so calling it says nothing".formatted(name));
    }
}
