package svit.web;

public interface BeanConfigurer<T> {

    T configure(T bean);

}
