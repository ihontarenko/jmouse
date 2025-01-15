package test.application;

import svit.beans.BeanContext;
import svit.beans.BeanContextAware;
import svit.beans.annotation.BeanInitializer;
import svit.beans.annotation.BeanName;
import svit.beans.annotation.Dependency;

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

    @Override
    public String getName() {
        return beanContext.getBean(User.class, "admin").getName();
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
