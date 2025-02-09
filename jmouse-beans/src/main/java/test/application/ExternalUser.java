package test.application;

import org.jmouse.beans.BeanContext;
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

    public void setParameters(String name, int count, User user, InternalUser user2, BeanContext context) {

    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public InternalUser getUser() {
        return user;
    }

    public void setUser(InternalUser user) {
        this.user = user;
    }
}
