package org.jmouse.template.extension;

public interface Function {

    Object execute(Object[] arguments);

    String getName();

}
