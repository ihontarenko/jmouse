package org.jmouse.el.extension.test;

import org.jmouse.el.extension.Test;

abstract public class AbstractTest implements Test {

    @Override
    public String toString() {
        return "Test: " + getName();
    }

}
