package svit.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.container.definition.ObjectFactoryBeanDefinition;
import svit.container.example.InternalUser;
import svit.container.example.User;

import java.util.Random;

public class Example {

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext();
        context.addInitializer(new ConfigurationBeanProviderBeanContextInitializer(Example.class));
        context.refresh();

        ObjectFactory<String> stringObjectFactory = () -> "Jane Doe";
        ObjectFactory<User> userObjectFactory = InternalUser::new;

        context.registerBean(Integer.class, 123);
        context.registerBean(Integer.class, 123);
        context.registerBean(Integer.class, 123);
        context.registerBean(Integer.class, 123);

        context.registerBean("jane", stringObjectFactory, BeanScope.PROTOTYPE);
        context.registerBean("userPrototype", userObjectFactory, BeanScope.PROTOTYPE);
        context.registerBean("userPrototype", userObjectFactory, BeanScope.PROTOTYPE);
        context.registerBean("userPrototype", userObjectFactory, BeanScope.PROTOTYPE);
        context.registerBean("userPrototype", userObjectFactory, BeanScope.PROTOTYPE);

        System.out.println(
                context.getBean("jane").toString()
        );

        context.registerDefinition(new ObjectFactoryBeanDefinition("random", Integer.class, () -> new Random().nextInt()));

        System.out.println(context.getBean("random").toString());
        System.out.println(context.getBean("doubleRandom").toString());
        System.out.println(context.getBean("doubleRandom").toString());


        for (int i = 0; i < 10; i++) {
            System.out.println(
                    context.getBean( "userPrototype").toString()
            );
        }

        System.out.println(context.getBean("gal").toString());

//        User user = context.getBean(User.class, "internal_user");
//        System.out.println(user.getName());
        System.out.println(context.getBean(String.class, "upper"));
    }

}
