package org.jmouse.el.extension.test;

import org.jmouse.el.extension.Test;

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
