package svit.beans;

import org.jmouse.svit.example.User;
import org.jmouse.svit.example.Utils;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext();
        context.refresh();
//        context.setBeanContainerRegistry(globalsContainerRegistry);
        context.addInitializer(new ScannerBeanContextInitializer(User.class));
        context.refresh();

        context.registerBean("bean123", () -> 123, BeanScope.SINGLETON);

        System.out.println(
                context.getBean(Utils.class).getOsName()
        );

        System.out.println(context.getBean("bean123").toString());

        System.out.println("end");
    }

}
