package org.jmouse.testing_ground.descriptive;

import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.metadata.object.JavaBeanDescriptor;
import org.jmouse.core.metadata.ClassDescriptor;
import org.jmouse.core.metadata.MetaDescriptor;
import org.jmouse.core.metadata.object.ObjectDescriptor;
import org.jmouse.testing_ground.example.WebServer;
import org.jmouse.web.server.WebServers;
import org.jmouse.testing_ground.beancontext.application.ExternalUser;
import org.jmouse.testing_ground.beancontext.application.InternalUser;
import org.jmouse.testing_ground.beancontext.application.UserHolder;

public class Example {

    public static void main(String... arguments) {
        JavaBean<InternalUser> bean = JavaBean.of(InternalUser.class);

        ClassDescriptor descriptorA = MetaDescriptor.forClass(InternalUser.class);
        ClassDescriptor descriptorB = MetaDescriptor.forClass(ExternalUser.class);
        ClassDescriptor descriptorC = MetaDescriptor.forClass(WebServers.class);
        ClassDescriptor descriptorD = MetaDescriptor.forClass(WebServer.class);
        ClassDescriptor descriptorE = MetaDescriptor.forClass(UserHolder.class);

        ExternalUser externalUser = new ExternalUser();

        externalUser.setUser(new InternalUser());

        ObjectDescriptor<ExternalUser> descriptor = MetaDescriptor.forBean(ExternalUser.class, externalUser);
        ObjectDescriptor<UserHolder> recordBean = MetaDescriptor.forValueObject(UserHolder.class);

        descriptor.getProperty("user").getGetter();

        System.out.println(recordBean);
    }

}
