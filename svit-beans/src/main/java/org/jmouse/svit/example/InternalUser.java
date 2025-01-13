package org.jmouse.svit.example;

import svit.beans.BeanContext;
import svit.beans.annotation.Dependency;

public class InternalUser implements User {

    @Dependency
    private BeanContext beanContext;

    @Dependency("defaultUserName")
    private String name;

    public InternalUser() {
        System.out.println("InternalUser: Initialization");
    }

    @Override
    public String getName() {
        return beanContext.getBean(User.class, "external_user").getName();
    }
}
