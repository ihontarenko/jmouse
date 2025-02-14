package org.jmouse.testing_ground.beancontext.application;

import org.jmouse.beans.annotation.Dependency;

public class AdminUser implements User {

    @Dependency("gal")
    private String name;

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
