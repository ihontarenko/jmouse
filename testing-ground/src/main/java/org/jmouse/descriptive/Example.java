package org.jmouse.descriptive;

import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.metadata.BeanDescriptor;
import org.jmouse.core.metadata.ClassDescriptor;
import org.jmouse.core.metadata.MetaDescriptor;
import org.jmouse.example.WebServer;
import org.jmouse.web.server.WebServers;
import test.application.ExternalUser;
import test.application.InternalUser;
import test.application.UserHolder;

public class Example {

    public static void main(String... arguments) {
        JavaBean<InternalUser> bean = JavaBean.of(InternalUser.class);

        ClassDescriptor descriptorA = MetaDescriptor.forClass(InternalUser.class);
        ClassDescriptor descriptorB = MetaDescriptor.forClass(ExternalUser.class);
        ClassDescriptor descriptorC = MetaDescriptor.forClass(WebServers.class);
        ClassDescriptor descriptorD = MetaDescriptor.forClass(WebServer.class);
        ClassDescriptor descriptorE = MetaDescriptor.forClass(UserHolder.class);

        ExternalUser externalUser = new ExternalUser();

        BeanDescriptor<ExternalUser> descriptor = MetaDescriptor.forBean(ExternalUser.class);
        BeanDescriptor<UserHolder> recordBean = MetaDescriptor.forValueObject(UserHolder.class);


        System.out.println(recordBean);
    }

}
