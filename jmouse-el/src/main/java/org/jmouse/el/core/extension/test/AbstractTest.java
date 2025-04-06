package org.jmouse.el.core.extension.test;

import org.jmouse.el.core.extension.Test;

/**
 * An abstract base class for test extensions used in expression evaluation.
 */
abstract public class AbstractTest implements Test {

    /**
     * Returns a string representation of the test.
     */
    @Override
    public String toString() {
        return "Test: " + getName();
    }
}
