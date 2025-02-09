package org.jmouse.descriptive;

import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.metadata.ClassDescriptor;
import org.jmouse.core.metadata.MetaDescriptor;
import test.application.InternalUser;

public class Example {

    public static void main(String... arguments) {
        JavaBean<InternalUser> bean = JavaBean.of(InternalUser.class);

        ClassDescriptor descriptor = MetaDescriptor.forClass(InternalUser.class);

        System.out.println(bean);
    }

}
