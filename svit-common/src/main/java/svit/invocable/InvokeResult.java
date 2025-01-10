package svit.invocable;

import svit.context.AbstractResultContext;

import static java.util.Objects.nonNull;

public class InvokeResult extends AbstractResultContext {

    @Override
    public String toString() {
        return "InvokeResult: hasReturnValue(%s), hasError(%s)".formatted(nonNull(getReturnValue()), hasErrors());
    }

}
