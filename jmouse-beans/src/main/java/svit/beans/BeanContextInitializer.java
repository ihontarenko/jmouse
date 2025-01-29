package svit.beans;

/**
 * Interface for initializing a {@link BeanContext}.
 * Implementations of this interface are responsible for configuring and preparing the {@link BeanContext}
 * before it is used.
 */
@FunctionalInterface
public interface BeanContextInitializer {

    /**
     * Initializes the given {@link BeanContext}.
     *
     * @param context the {@link BeanContext} to initialize.
     */
    void initialize(BeanContext context);
}
