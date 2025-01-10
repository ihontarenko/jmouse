package svit.container.definition;

import svit.container.BeanCreationType;
import svit.container.Lifecycle;
import svit.container.instantiation.BeanInstantiationStrategy;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

/**
 * Describes a bean's metadata and lifecycle within the container,
 * including its name, class, creation strategy, dependencies, and annotations.
 * <p>
 * A {@code BeanDefinition} can also have parent-child relationships,
 * allowing hierarchical organization of definitions.
 */
public interface BeanDefinition {

    /**
     * Determines if this definition represents a singleton or non-bean lifecycle.
     *
     * @return {@code true} if the bean is {@link Lifecycle#SINGLETON} or {@link Lifecycle#NON_BEAN},
     *         otherwise {@code false}.
     */
    default boolean isSingleton() {
        return getLifecycle() == Lifecycle.SINGLETON || getLifecycle() == Lifecycle.NON_BEAN;
    }

    /**
     * Determines if this definition represents a prototype lifecycle.
     *
     * @return {@code true} if the bean is not a singleton (prototype or other scopes),
     *         otherwise {@code false}.
     */
    default boolean isPrototype() {
        return !isSingleton();
    }

    /**
     * Retrieves the name of the bean.
     *
     * @return the bean name.
     */
    String getBeanName();

    /**
     * Sets the name of the bean.
     *
     * @param name the new bean name.
     */
    void setBeanName(String name);

    /**
     * Retrieves the class type of the bean.
     *
     * @return the bean class.
     */
    Class<?> getBeanClass();

    /**
     * Sets the class type of the bean.
     *
     * @param type the new bean class.
     */
    void setBeanClass(Class<?> type);

    /**
     * Retrieves the parent definition of this bean, if any.
     *
     * @return the parent {@link BeanDefinition}, or {@code null} if none.
     */
    BeanDefinition getParentDefinition();

    /**
     * Sets the parent definition of this bean.
     *
     * @param parent the parent {@link BeanDefinition}.
     */
    void setParentDefinition(BeanDefinition parent);

    /**
     * Retrieves the list of children definitions associated with this bean.
     *
     * @return a list of child {@link BeanDefinition} instances.
     */
    List<BeanDefinition> getChildrenDefinitions();

    /**
     * Adds a child definition to this bean.
     *
     * @param child the child {@link BeanDefinition} to add.
     */
    void addChildDefinition(BeanDefinition child);

    /**
     * Retrieves the lifecycle scope of this bean.
     *
     * @return the {@link Lifecycle} of the bean.
     */
    Lifecycle getLifecycle();

    /**
     * Sets the lifecycle scope of this bean.
     *
     * @param lifecycle the new {@link Lifecycle}.
     */
    void setLifecycle(Lifecycle lifecycle);

    /**
     * Retrieves all dependencies declared by this bean.
     *
     * @return a list of {@link BeanDependency} objects.
     */
    List<BeanDependency> getBeanDependencies();

    /**
     * Retrieves the creation type for this bean, indicating how it should be instantiated.
     *
     * @return a {@link BeanCreationType} value.
     */
    BeanCreationType getBeanCreationType();

    /**
     * Retrieves the actual bean instance if one has been created.
     *
     * @param <T> the type of the bean.
     * @return the bean instance, or {@code null} if not created yet.
     */
    <T> T getBeanInstance();

    /**
     * Sets the actual bean instance.
     *
     * @param instance the new bean instance.
     */
    void setBeanInstance(Object instance);

    /**
     * Retrieves the strategy used to instantiate this bean.
     *
     * @return the {@link BeanInstantiationStrategy}.
     */
    BeanInstantiationStrategy getBeanCreationStrategy();

    /**
     * Sets the strategy used to instantiate this bean.
     *
     * @param strategy the {@link BeanInstantiationStrategy} to set.
     */
    void setBeanCreationStrategy(BeanInstantiationStrategy strategy);

    /**
     * Retrieves the annotations present on this bean definition.
     *
     * @return a collection of annotations.
     */
    Collection<Annotation> getAnnotations();

    /**
     * Sets the annotations present on this bean definition.
     *
     * @param annotations a collection of annotations to set.
     */
    void setAnnotations(Collection<Annotation> annotations);

    /**
     * Checks if this bean definition is annotated with the specified class.
     *
     * @param annotatedClass the annotation type to check.
     * @return {@code true} if annotated, otherwise {@code false}.
     */
    boolean isAnnotatedWith(Class<?> annotatedClass);
}
