package org.jmouse.el.extension;

public interface Function {

    Object execute(Object[] arguments);

    String getName();

}
