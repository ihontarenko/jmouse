package test.application;

import org.jmouse.testing_ground.beans.BeanContext;
import org.jmouse.testing_ground.beans.BeanContextAware;
import org.jmouse.testing_ground.beans.annotation.BeanInitializer;
import org.jmouse.testing_ground.beans.annotation.BeanName;
import org.jmouse.testing_ground.beans.annotation.Dependency;

@BeanName("client")
public class InternalUser implements User, BeanContextAware {

    @Dependency
    private BeanContext beanContext;

    @Dependency("defaultUserName")
    private String name;

    @BeanInitializer
    public void init() {
        System.out.println("init()...");
    }

    public String getExternalName() {
        return beanContext.getBean(User.class, "admin").getName();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the {@link BeanContext} for this component.
     *
     * @param context the {@link BeanContext} to set.
     */
    @Override
    public void setBeanContext(BeanContext context) {

    }

    /**
     * Retrieves the {@link BeanContext} associated with this component.
     *
     * @return the current {@link BeanContext}.
     */
    @Override
    public BeanContext getBeanContext() {
        return null;
    }
}
