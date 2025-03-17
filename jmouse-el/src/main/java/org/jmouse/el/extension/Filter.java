package org.jmouse.el.extension;

public interface Filter {

    Object apply(Object input);

    String getName();

}
