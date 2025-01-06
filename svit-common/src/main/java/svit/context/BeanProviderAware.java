package svit.context;

public interface BeanProviderAware {
    void setBeanProvider(BeanProvider beanProvider);
    BeanProvider getBeanProvider();
}
