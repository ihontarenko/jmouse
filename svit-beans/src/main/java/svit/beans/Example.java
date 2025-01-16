package svit.beans;

import test.application.TestRoot;
import test.application.User;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext(TestRoot.class, BeanContext.class);
        context.refresh();
        context.addInitializer(Example::selfInitialize);
        context.addInitializer(new ScannerBeanContextInitializer(User.class));
        context.refresh();

        context.registerBean("ololo", Example::getValue);

        System.out.println("end");
    }

    public static void selfInitialize(BeanContext context) {
        System.out.println(context);
    }

    public static String getValue() {
        return Example.class.getName();
    }

}
