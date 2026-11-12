package org.jmouse.web.mvc.result;

import java.util.Objects;
import org.jmouse.core.MethodParameter;

/** An invocation outcome. Route metadata and mutable view state belong to the request, not this value. */
public sealed interface HandlerResult permits HandlerResult.ReturnValue, HandlerResult.Handled {
    static ReturnValue returned(Object value, MethodParameter returnType) {
        return new ReturnValue(value, returnType);
    }

    static Handled handled() {
        return Handled.INSTANCE;
    }

    /** A method has returned; null values and void declarations are valid and remain distinguishable. */
    record ReturnValue(Object value, MethodParameter returnType) implements HandlerResult {
        public ReturnValue {
            Objects.requireNonNull(returnType, "Declared return type is required");
        }
    }

    /** A handler or resolver has already performed the HTTP response operation. */
    enum Handled implements HandlerResult {
        INSTANCE
    }
}
