package svit.beans.processor;

/**
 * Interface for classes that are aware of and can manage {@link BeanPostProcessor} instances.
 * <p>
 * This interface provides a mechanism for adding {@link BeanPostProcessor}s
 * to a class, enabling pre- and post-initialization processing of beans.
 * </p>
 *
 * @see BeanPostProcessor
 */
public interface BeanPostProcessorAware {

    /**
     * Adds a {@link BeanPostProcessor} to the class implementing this interface.
     * <p>
     * The processor will be used for pre- and post-initialization operations
     * on beans within the implementing class.
     * </p>
     *
     * @param processor the {@link BeanPostProcessor} to add.
     */
    void addBeanPostProcessor(BeanPostProcessor processor);
}
