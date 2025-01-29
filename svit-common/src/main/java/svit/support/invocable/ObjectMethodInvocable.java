package svit.support.invocable;

import java.lang.reflect.Method;

public class ObjectMethodInvocable extends AbstractInvocable {

    private final Object targetObject;

    public ObjectMethodInvocable(Object targetObject, String methodName, Class<?>... parameterTypes) {
        super(targetObject.getClass(), methodName, parameterTypes);
        this.targetObject = targetObject;
    }

    public ObjectMethodInvocable(Object targetObject, Method method) {
        super(method);
        this.targetObject = targetObject;
    }

    @Override
    public Object getTarget() {
        return this.targetObject;
    }

    @Override
    public InvokeResult invoke() {
        validateParameters();
        return Invoker.invoke(this);
    }

}
