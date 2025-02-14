package org.jmouse.testing_ground.beancontext;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.testing_ground.beancontext.application.AdminUser;
import org.jmouse.testing_ground.beancontext.application.GetValue;
import org.jmouse.web.Launcher;
import org.jmouse.testing_ground.beancontext.application.User;
import org.jmouse.testing_ground.beancontext.application.UserHolder;

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

        context.getDefinition("adminUser").setProxied(true);

        System.out.println(context.getBean(AdminUser.class));

        context.getBeans(User.class);
        GetValue getValue = context.getBean(GetValue.class);
        System.out.println(getValue.getObject());
    }


}
