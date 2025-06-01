package org.jmouse.testing_ground.beancontext.application;

import org.jmouse.beans.annotation.Dependency;

import java.util.Collection;

public class AdminUser implements User {

    @Dependency("gal")
    private String name;

    @Dependency
    private Collection<Float> floats;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AdminUser{" +
                "name='" + name + '\'' +
                '}';
    }

    interface User {
        String getName();
    }

}
