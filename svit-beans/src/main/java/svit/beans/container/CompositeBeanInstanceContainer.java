package svit.beans.container;

import svit.beans.*;

public class CompositeBeanInstanceContainer implements DelegatingBeanContainer {

    @Override
    public <T> T getBean(String name) {
        return null;
    }


    @Override
    public void registerBean(String name, Object bean) {

    }


    @Override
    public BeanContainer getBeanContainer(Scope scope) {
        return null;
    }


    @Override
    public void registerBeanContainer(Scope scope, BeanContainer container) {

    }


    @Override
    public void removeBeanInstanceContainers() {

    }

    @Override
    public boolean supports(Scope scope) {
        return false;
    }


    @Override
    public boolean containsBean(String name) {
        return false;
    }

}
