package svit.container.processor;

public interface BeanPostProcessorAware {

    void addBeanPostProcessor(BeanPostProcessor processor);

}
