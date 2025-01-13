package svit.beans;

import org.jmouse.svit.example.ExternalUser;
import org.jmouse.svit.example.User;

public class Example {

    public static void main(String[] args) {

        BeanContext context = new DefaultBeanContext();
        context.addInitializer(new ScannerBeanContextInitializer(User.class));
        context.refresh();

        context.registerBean("bean123", () -> 123, BeanScope.PROTOTYPE);
        context.registerBean("userPrototype", ExternalUser::new, BeanScope.PROTOTYPE);

        context.addInitializer(ctx -> ctx.registerBean("test1", "bean added in external initializer"));

        context.refresh();

        System.out.println("BEAN VALUE: " + context.<User>getBean( "userPrototype").getName());
        System.out.println("BEAN VALUE: " + context.getBean( "test1"));

        System.out.println("end!");
    }

}
