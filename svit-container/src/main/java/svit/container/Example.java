package svit.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svit.container.definition.ObjectFactoryBeanDefinition;
import svit.container.example.User;

import java.util.Random;

public class Example {

    private static final Logger LOGGER = LoggerFactory.getLogger(Example.class);

    public static void main(String[] args) {
        BeanContext context = new DefaultBeanContext();
        context.addInitializer(new ConfigurationBeanProviderBeanContextInitializer(Example.class));
        context.refresh();

        context.registerBean(Integer.class, 123);
        context.registerBean(String.class, "John Doe");
        context.registerBean(String.class, "Jane Doe", Lifecycle.PROTOTYPE);

        context.registerDefinition(new ObjectFactoryBeanDefinition("random", Integer.class, () -> new Random().nextInt()));

        System.out.println(context.getBean("random").toString());
        System.out.println(context.getBean("random").toString());
        System.out.println(context.getBean("random").toString());

        User user = context.getBean(User.class, "internal_user");

        System.out.println(context.getBean("gal").toString());

        System.out.println(user.getName());
        System.out.println(context.getBean(String.class, "upper"));
    }

}
