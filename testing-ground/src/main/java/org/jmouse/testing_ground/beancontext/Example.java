package org.jmouse.testing_ground.beancontext;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.ScannerBeanContextInitializer;
import org.jmouse.context.ApplicationConfigurer;
import org.jmouse.web.Launcher;
import org.jmouse.testing_ground.beancontext.application.User;
import org.jmouse.testing_ground.beancontext.application.UserHolder;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext(User.class, BeanContext.class, Launcher.class);
        context.refresh();
        context.addInitializer(new ScannerBeanContextInitializer());
        context.refresh();

        context.getBeans(User.class);

        System.out.println(
                context.getBeans(UserHolder.class)
        );

        System.out.println(
                context.getBeans(ApplicationConfigurer.class)
        );
    }


}
