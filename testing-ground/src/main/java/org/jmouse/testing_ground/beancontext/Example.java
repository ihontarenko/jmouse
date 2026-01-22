package org.jmouse.testing_ground.beancontext;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.core.bind.PropertyAccessor;
import org.jmouse.core.bind.accessor.JavaBeanAccessor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanIntrospector;
import org.jmouse.testing_ground.beancontext.application.AdminUser;
import org.jmouse.testing_ground.beancontext.application.GetValue;
import org.jmouse.web.Launcher;
import org.jmouse.testing_ground.beancontext.application.User;
import org.jmouse.testing_ground.beancontext.application.UserHolder;

import java.util.Map;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext(User.class, BeanContext.class, Launcher.class);
        context.refresh();
        context.addInitializer(new ScannerBeanContextInitializer());
        context.refresh();

        AdminUser adminUser = new AdminUser();

        adminUser.setName("admin");

        System.out.println(adminUser);

//        context.registerBean("adminUser", () -> adminUser);
        context.registerBean("adminUser", adminUser, BeanScope.PROTOTYPE);

        context.getDefinition("adminUser").setProxied(false);

        System.out.println(context.getBean(AdminUser.class));

        context.getBeans(User.class);
        GetValue getValue = context.getBean(GetValue.class);
        System.out.println(getValue.getObject());
    }


}
