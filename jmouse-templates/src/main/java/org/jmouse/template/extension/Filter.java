package org.jmouse.template.extension;

public interface Filter {

    Object apply(Object input);

    String getName();

}
