package org.jmouse.beans;

import test.application.TestRoot;
import test.application.User;

import java.util.List;

public class Example {

    private List<Integer> integers;

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext(TestRoot.class, BeanContext.class);

        context.refresh();

        context.addInitializer(Example::selfInitialize);
        context.addInitializer(new ScannerBeanContextInitializer(User.class));

        context.refresh();

        context.registerBean("ololo", Example::getValue);

        System.out.println(
                context.getBean("list").toString()
        );
    }

    public static void selfInitialize(BeanContext context) {
        context.registerBean("list", new Example()::getNames);
    }

    public static String getValue() {
        return Example.class.getName();
    }

    public List<String> getNames() {
        return List.of("test");
    }

}
