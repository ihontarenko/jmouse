package org.jmouse.action.smoke;

import org.jmouse.action.TypedAction;
import org.jmouse.core.scope.Context;

public class SourceTarget implements TypedAction<Object> {

    private Object source;
    private Object target;

    @Override
    public Object execute(Context context) {
        System.out.println(source + " -> " + target);
        return null;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public void setTarget(Object target) {
        this.target = target;
    }
}