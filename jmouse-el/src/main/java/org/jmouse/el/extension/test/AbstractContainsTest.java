package org.jmouse.el.extension.test;

import org.jmouse.core.support.ArraySupport;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;
import java.util.List;

abstract public class AbstractContainsTest extends AbstractTest {

    protected Collection<?> toExpected(Arguments arguments) {
        if (arguments.isEmpty()) {
            return List.of();
        }

        Object first = arguments.getFirst();

        if (arguments.size() == 1) {
            if (first instanceof Collection<?> collection) {
                return collection;
            }
            if (ArraySupport.isArray(first)) {
                return ArraySupport.toList(first);
            }
            if (first instanceof CharSequence charSequence) {
                return ArraySupport.toCodePoints(charSequence.toString());
            }
        }

        return arguments.toList();
    }

}
