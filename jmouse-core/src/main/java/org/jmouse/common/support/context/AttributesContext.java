package org.jmouse.common.support.context;

public interface AttributesContext {

    <T> T requireAttribute(Object name);

    <T> T getAttribute(Object name);

    void setAttribute(Object name, Object argument);

    void setAttribute(Object argument);

    void setAttributes(Object... arguments);

    boolean hasAttribute(Object name);

}
