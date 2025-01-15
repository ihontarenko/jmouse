package svit.beans;

import test.application.TestRoot;
import test.application.User;
import test.application.Utils;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext(TestRoot.class, BeanContext.class);
        context.refresh();
        context.addInitializer(new ScannerBeanContextInitializer(User.class));
        context.refresh();

        context.registerBean("bean123", () -> 123, BeanScope.SINGLETON);

        System.out.println(context.getBean("bean123").toString());
        System.out.println(context.getBean(Utils.class).getOsName());
        System.out.println(context.getBean(User.class, "client").getName());

        System.out.println("end");
    }

}
