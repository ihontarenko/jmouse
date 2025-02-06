package test.application;

import org.jmouse.beans.annotation.BeanName;
import org.jmouse.beans.annotation.Dependency;
import org.jmouse.beans.annotation.BeanInitializer;

@BeanName("admin")
public class ExternalUser implements User {

    @Dependency("gal")
    private String name;

    @Dependency("int123")
    private Integer count;

    @Dependency("client")
    private InternalUser user;

    @BeanInitializer
    public void init() {
        System.out.println("ExternalUser: Initialization");
    }

    @Override
    public String getName() {
        return name;
    }

}
