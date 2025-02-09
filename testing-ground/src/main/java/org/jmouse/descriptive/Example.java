package org.jmouse.descriptive;

import org.jmouse.core.bind.JavaBean;
import org.jmouse.core.descriptive.ClassDescriptor;
import org.jmouse.core.descriptive.Descriptive;
import test.application.InternalUser;

import java.nio.file.attribute.UserPrincipal;

public class Example {

    public static void main(String... arguments) {

        JavaBean<UserPrincipal> bean = JavaBean.of(UserPrincipal.class);

        ClassDescriptor descriptor = Descriptive.forClass(InternalUser.class);

        System.out.println(bean);

    }

}
