package org.jmouse.testing_ground.beancontext.application;

import org.jmouse.beans.annotation.Provide;

@Provide(proxied = true, value = "root")
public class TestRoot implements GetValue {

    public String getValue() {
        return getClass().getName();
    }

    public Object getObject() {
        return 123;
    }

}
