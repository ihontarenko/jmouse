package org.jmouse.el.extension.filter;

import org.jmouse.el.extension.Filter;

abstract public class AbstractFilter implements Filter {

    @Override
    public String toString() {
        return "Filter: " + getName();
    }

}
