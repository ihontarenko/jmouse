package org.jmouse.core.bind;

public interface ObjectAccessorProvider {

    boolean supports(Object source);

    ObjectAccessor create(Object source);

}
