package svit.beans;

import test.application.User;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext();

        context.refresh();
        context.addInitializer(new ScannerBeanContextInitializer(User.class));
        context.refresh();

        context.registerBean("bean123", () -> 123, BeanScope.SINGLETON);

        System.out.println(context.getBean("bean123").toString());

        System.out.println("end");
    }

}
