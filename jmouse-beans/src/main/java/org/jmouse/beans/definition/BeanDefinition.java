package org.jmouse.beans.definition;

import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.Scope;
import org.jmouse.beans.instantiation.BeanInstantiationStrategy;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

/**
 * Describes a bean's descriptor and lifecycle within the container,
 * including its name, class, creation strategy, dependencies, and annotations.
 * <p>
 * A {@code BeanDefinition} can also have parent-child relationships,
 * allowing hierarchical organization of definitions.
 */
public interface BeanDefinition {

    /**
     * Determines if this definition represents a singleton or non-bean lifecycle.
     *
     * @return {@code true} if the bean is {@link BeanScope#SINGLETON} or {@link BeanScope#NON_BEAN},
     *         otherwise {@code false}.
     */
    default boolean isSingleton() {
        return getScope() == BeanScope.SINGLETON;
    }

    /**
     * Determines if this definition represents a prototype lifecycle.
     *
     * @return {@code true} if the bean is not a singleton (prototype or other scopes),
     *         otherwise {@code false}.
     */
    default boolean isPrototype() {
        return getScope() == BeanScope.PROTOTYPE;
    }

    /**
     * Indicates whether the bean represented by this definition is primary.=
     *
     * @return {@code true} if the bean is primary, otherwise {@code false}.
     */
    default boolean isPrimary() {
        return false;
    }

    /**
     * Sets whether the bean represented by this definition is primary.
     *
     * @param primary {@code true} if the bean should be primary, {@code false} otherwise.
     */
    void setPrimary(boolean primary);

    /**
     * Indicates whether the bean represented by this definition is proxied.
     * <p>
     * A proxied bean typically wraps the actual bean instance with additional functionality,
     * such as method interception or lazy initialization.
     *
     * @return {@code true} if the bean is proxied, otherwise {@code false}.
     */
    default boolean isProxied() {
        return false;
    }

    /**
     * Sets whether the bean represented by this definition is proxied.
     *
     * @param proxied {@code true} if the bean should be proxied, {@code false} otherwise.
     */
    void setProxied(boolean proxied);

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
     * Returns a list of parameterized types associated with this bean definition.
     * Useful when the bean class is a generic type, and its parameters need to be
     * tracked for reflection or dependency injection purposes.
     *
     * @return a list of parameterized types
     */
    List<Class<?>> getParametrizedTypes();

    /**
     * Sets the list of parameterized types for this bean definition. This is
     * particularly helpful when dealing with generic types, allowing you to
     * specify or update the actual type parameters used by the bean.
     *
     * @param types a list of parameterized types
     */
    void setParametrizedTypes(List<Class<?>> types);

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
     * @return the {@link Scope} of the bean.
     */
    Scope getScope();

    /**
     * Sets the scope of this bean.
     *
     * @param scope the new {@link BeanScope}.
     */
    void setScope(Scope scope);

    /**
     * Retrieves all dependencies declared by this bean.
     *
     * @return a list of {@link BeanDependency} objects.
     */
    List<BeanDependency> getBeanDependencies();

    /**
     * Retrieves the creation type for this bean, indicating how it should be instantiated.
     *
     * @return a {@link BeanInstantiationType} value.
     */
    BeanInstantiationType getInstantiationType();

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
     * Retrieves a specific annotation of the given type from this bean definition.
     *
     * @param <A> the type of the annotation to retrieve
     * @param annotationType the class bean of the annotation type
     * @return the annotation instance if present, otherwise {@code null}
     */
    <A extends Annotation> A getAnnotation(Class<A> annotationType);

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
