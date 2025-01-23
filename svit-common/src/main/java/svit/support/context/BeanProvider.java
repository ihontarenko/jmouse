package svit.support.context;

public interface BeanProvider {

    <T> T getBean(Class<T> beanClass);

    <T> T getBean(String beanName, Class<T> beanClass);

}
