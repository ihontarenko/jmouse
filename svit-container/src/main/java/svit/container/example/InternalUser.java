package svit.container.example;

import svit.container.BeanContext;
import svit.container.annotation.Dependency;

public class InternalUser implements User {

    @Dependency
    private BeanContext beanContext;

    @Dependency("defaultUserName")
    private String name;

    @Dependency("external_user")
    private ExternalUser user;

    public InternalUser() {
        System.out.println("InternalUser: Initialization");
    }

    @Override
    public String getName() {
        return beanContext.getBean(User.class, "external_user").getName();
    }
}
