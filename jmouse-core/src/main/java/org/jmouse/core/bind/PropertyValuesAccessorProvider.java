package org.jmouse.core.bind;

public interface PropertyValuesAccessorProvider {

    boolean supports(Object source);

    ObjectAccessor create(Object source);

}
