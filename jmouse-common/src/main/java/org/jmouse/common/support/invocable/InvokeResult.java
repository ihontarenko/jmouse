package org.jmouse.common.support.invocable;

import org.jmouse.core.scope.AbstractResultContext;

import static java.util.Objects.nonNull;

public class InvokeResult extends AbstractResultContext {

    @Override
    public String toString() {
        return "InvokeResult: hasReturnValue(%s), hasError(%s)".formatted(nonNull(getReturnValue()), hasErrors());
    }

}
