package svit.invocable;

import svit.context.ErrorDetails;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final public class Invoker {

    public static final String ERROR_CODE = "INVOCATION_ERROR";

    public static InvokeResult invoke(Invocable invocable) {
        InvokeResult result = new InvokeResult();

        try {
            Method method = invocable.getDescriptor().getNativeMethod();
            result.setReturnValue(method.invoke(invocable.getTarget(), invocable.getPreparedParameters()));
        } catch (IllegalAccessException | InvocationTargetException e) {
            Throwable exception = e;

            if (exception.getMessage() == null) {
                exception = e.getCause();
            }

            result.addError(new ErrorDetails(ERROR_CODE, exception.getMessage(), exception));
        }

        return result;
    }

}
