package org.jmouse.core.invoke;

import org.jmouse.core.scope.AbstractVariablesContext;
import org.jmouse.core.scope.Context;

public class InvocationMethodContext extends AbstractVariablesContext {

    public static Context createDefault() {
        return new InvocationMethodContext();
    }

}
