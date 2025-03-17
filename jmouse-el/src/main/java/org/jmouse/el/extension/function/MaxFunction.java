package org.jmouse.el.extension.function;

import org.jmouse.el.extension.Function;

public class MaxFunction implements Function {

    @Override
    public Object execute(Object[] arguments) {
        int a = (int) arguments[0];
        int b = (int) arguments[1];

        return Math.max(a, b);
    }

    @Override
    public String getName() {
        return "max";
    }
}
