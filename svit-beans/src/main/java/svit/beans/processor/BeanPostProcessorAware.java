package svit.beans.processor;

public interface BeanPostProcessorAware {

    void addBeanPostProcessor(BeanPostProcessor processor);

}
