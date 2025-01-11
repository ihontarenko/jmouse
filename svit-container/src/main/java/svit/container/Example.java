package svit.container;

import svit.container.example.InternalUser;
import svit.container.example.User;

import java.util.Random;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext();
        context.addInitializer(new ConfigurationBeanProviderBeanContextInitializer(Example.class));
        context.refresh();
        context.refresh();

        ObjectFactory<User>   userObjectFactory = InternalUser::new;

        context.registerBean("userPrototype", userObjectFactory, BeanScope.PROTOTYPE);

        System.out.println("BEAN VALUE: " + context.<User>getBean( "userPrototype").getName());
        System.out.println("BEAN VALUE: " + context.<User>getBean( "userPrototype").getName());
        System.out.println("BEAN VALUE: " + context.<User>getBean( "userPrototype").getName());
        System.out.println("BEAN VALUE: " + context.<User>getBean( "userPrototype").getName());
    }

}
