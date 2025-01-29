package svit.support.invocable;

import java.util.Collection;

public interface Invocable {

    InvokeResult invoke();

    void addParameter(MethodParameter parameter);

    default void addParameters(Collection<?> parameters) {
        int index = getParameters().size();
        for (Object parameter : parameters) {
            addParameter(new MethodParameter(index++, parameter));
        }
    }

    Collection<? extends MethodParameter> getParameters();

    default void cleanupParameters() {
        if (getParameters() != null) {
            getParameters().clear();
        }
    }

    Object[] getPreparedParameters();

    MethodDescriptor getDescriptor();

    default Object getTarget() {
        return null;
    }

}
