package svit.beans;

import test.application.TestRoot;
import test.application.User;
import test.application.Utils;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext(TestRoot.class);
        context.setBaseClasses();
        context.refresh();
        context.addInitializer(new ScannerBeanContextInitializer(User.class));
        context.refresh();

        context.registerBean("bean123", () -> 123, BeanScope.SINGLETON);

        System.out.println(context.getBean("bean123").toString());
        System.out.println(context.getBean(Utils.class).getOsName());

        System.out.println("end");
    }

}
