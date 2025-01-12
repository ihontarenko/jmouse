package svit.beans;

import svit.beans.example.ExternalUser;
import svit.beans.example.User;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext();
        context.refresh();

        context.registerBean("userPrototype", ExternalUser::new, BeanScope.PROTOTYPE);

        context.addInitializer(new ConfigurationBeanProviderBeanContextInitializer(Example.class));
        context.addInitializer(ctx -> ctx.registerBean("test1", "хуй"));

        context.refresh();

        System.out.println("BEAN VALUE: " + context.<User>getBean( "userPrototype").getName());
        System.out.println("BEAN VALUE: " + context.getBean( "test1"));

        System.out.println("end!");
    }

}
