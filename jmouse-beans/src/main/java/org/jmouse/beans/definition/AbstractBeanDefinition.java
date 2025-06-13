package org.jmouse.beans.definition;

import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.Scope;
import org.jmouse.beans.instantiation.BeanInstantiationStrategy;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * An abstract base class for {@link BeanDefinition} implementations.
 * Manages common bean descriptor such as dependencies, lifecycle, annotations, and more.
 * <p>
 * This class serves as a foundation for concrete bean definitions in a container,
 * providing storage and access for parser properties:
 * <ul>
 *     <li>Bean name</li>
 *     <li>Bean class (type)</li>
 *     <li>Parent and child definitions</li>
 *     <li>Lifecycle scope</li>
 *     <li>Instantiation strategy and instance</li>
 *     <li>Dependencies and annotations</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyCustomBeanDefinition extends AbstractBeanDefinition {
 *     public MyCustomBeanDefinition(String name, Class<?> type) {
 *         super(name, type);
 *         // further initialization
 *     }
 *
 *     @Override
 *     public BeanCreationType getBeanCreationType() {
 *         return BeanCreationType.DEFAULT;
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractBeanDefinition implements BeanDefinition {

    /**
     * A list of dependencies that this bean requires.
     */
    protected final List<BeanDependency> dependencies;

    /**
     * A map of annotations (by their types) present on this bean definition.
     */
    protected final Map<Class<? extends Annotation>, Annotation> annotations;

    /**
     * The unique name of the bean.
     */
    protected String name;

    /**
     * The type (class) of the bean.
     */
    protected Class<?> type;

    protected List<Class<?>> parametrizedTypes;

    /**
     * Indicates whether the bean is proxied.
     */
    protected boolean proxied = false;

    /**
     * Indicates whether the bean is primary.
     */
    protected boolean primary = false;

    /**
     * The parent bean definition, if any.
     */
    protected BeanDefinition parent;

    /**
     * Child bean definitions that extend or complement this bean's definition.
     */
    protected List<BeanDefinition> children;

    /**
     * The lifecycle scope for this bean (e.g., SINGLETON, PROTOTYPE).
     */
    protected Scope scope;

    /**
     * The actual bean instance, if created.
     */
    protected Object instance;

    /**
     * The strategy for instantiating the bean.
     */
    protected BeanInstantiationStrategy strategy;

    /**
     * Constructs a new {@code AbstractBeanDefinition} with the specified name and type.
     *
     * @param name the unique name of the bean
     * @param type the bean's class
     */
    public AbstractBeanDefinition(String name, Class<?> type) {
        this.name = name;
        this.type = type;
        this.dependencies = new ArrayList<>();
        this.children = new ArrayList<>();
        this.annotations = new HashMap<>();
    }

    /**
     * Retrieves the name of this bean.
     *
     * @return the name of the bean
     */
    @Override
    public String getBeanName() {
        return name;
    }

    /**
     * Sets the name of this bean.
     *
     * @param name the new bean name
     */
    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the class of this bean.
     *
     * @return the bean's class
     */
    @Override
    public Class<?> getBeanClass() {
        return type;
    }

    /**
     * Sets the class of this bean.
     *
     * @param type the new class of the bean
     */
    @Override
    public void setBeanClass(Class<?> type) {
        this.type = type;
    }

    /**
     * Returns a list of parameterized types associated with this bean definition.
     * Useful when the bean class is a generic type, and its parameters need to be
     * tracked for reflection or dependency injection purposes.
     *
     * @return a list of parameterized types
     */
    @Override
    public List<Class<?>> getParametrizedTypes() {
        return List.copyOf(parametrizedTypes);
    }

    /**
     * Sets the list of parameterized types for this bean definition. This is
     * particularly helpful when dealing with generic types, allowing you to
     * specify or update the actual type parameters used by the bean.
     *
     * @param types a list of parameterized types
     */
    @Override
    public void setParametrizedTypes(List<Class<?>> types) {
        this.parametrizedTypes = List.copyOf(types);
    }

    /**
     * Indicates whether the bean represented by this definition is proxied.
     *
     * @return {@code true} if the bean is proxied, otherwise {@code false}.
     */
    @Override
    public boolean isProxied() {
        return proxied;
    }

    /**
     * Sets whether the bean represented by this definition is proxied.
     *
     * @param proxied {@code true} if the bean should be proxied, {@code false} otherwise.
     */
    @Override
    public void setProxied(boolean proxied) {
        this.proxied = proxied;
    }

    /**
     * Indicates whether the bean represented by this definition is primary.=
     *
     * @return {@code true} if the bean is primary, otherwise {@code false}.
     */
    @Override
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Sets whether the bean represented by this definition is primary.
     *
     * @param primary {@code true} if the bean should be primary, {@code false} otherwise.
     */
    @Override
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * Retrieves the creation type for this bean, indicating how it should be instantiated.
     *
     * @return a {@link BeanInstantiationType} value.
     */
    @Override
    public BeanInstantiationType getInstantiationType() {
        return null;
    }

    /**
     * Retrieves the parent definition of this bean, if any.
     *
     * @return the parent bean definition, or {@code null} if none
     */
    @Override
    public BeanDefinition getParentDefinition() {
        return parent;
    }

    /**
     * Sets the parent definition for this bean.
     *
     * @param parent the new parent bean definition
     */
    @Override
    public void setParentDefinition(BeanDefinition parent) {
        this.parent = parent;
    }

    /**
     * Retrieves the child definitions of this bean.
     *
     * @return a list of child definitions
     */
    @Override
    public List<BeanDefinition> getChildrenDefinitions() {
        return children;
    }

    /**
     * Adds a child definition to this bean.
     *
     * @param child the child definition to add
     */
    @Override
    public void addChildDefinition(BeanDefinition child) {
        children.add(child);
    }

    /**
     * Retrieves the lifecycle scope of this bean (e.g., SINGLETON, PROTOTYPE).
     *
     * @return the lifecycle scope
     */
    @Override
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the lifecycle scope of this bean.
     *
     * @param scope the new lifecycle scope
     */
    @Override
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * Retrieves the dependencies required by this bean.
     *
     * @return a list of bean dependencies
     */
    @Override
    public List<BeanDependency> getBeanDependencies() {
        return dependencies;
    }

    /**
     * Retrieves the actual instance of this bean, if it has been created.
     *
     * @param <T> the expected type of the bean
     * @return the bean instance, or {@code null} if not created
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBeanInstance() {
        return (T) instance;
    }

    /**
     * Sets the actual instance of this bean.
     *
     * @param instance the new bean instance
     */
    @Override
    public void setBeanInstance(Object instance) {
        this.instance = instance;
    }

    /**
     * Retrieves the instantiation strategy for this bean (e.g., constructor, method, bean factory).
     *
     * @return the bean instantiation strategy
     */
    @Override
    public BeanInstantiationStrategy getBeanCreationStrategy() {
        return strategy;
    }

    /**
     * Sets the instantiation strategy for this bean.
     *
     * @param strategy the new bean instantiation strategy
     */
    @Override
    public void setBeanCreationStrategy(BeanInstantiationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Retrieves the collection of annotations present on this bean definition.
     *
     * @return a collection of annotations
     */
    @Override
    public Collection<Annotation> getAnnotations() {
        return annotations.values();
    }

    /**
     * Retrieves a specific annotation of the given type from this bean definition.
     *
     * @param <A> the type of the annotation to retrieve
     * @param annotationType the class bean of the annotation type
     * @return the annotation instance if present, otherwise {@code null}
     */
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return (A) annotations.get(annotationType);
    }

    /**
     * Sets the annotations present on this bean definition.
     *
     * @param annotations a collection of annotations to associate with this bean
     */
    @Override
    public void setAnnotations(Collection<Annotation> annotations) {
        this.annotations.clear();
        for (Annotation annotation : annotations) {
            this.annotations.put(annotation.annotationType(), annotation);
        }
    }

    /**
     * Determines if this bean definition is annotated with the specified class.
     *
     * @param annotatedClass the annotation class to check
     * @return {@code true} if this bean is annotated with {@code annotatedClass}, otherwise {@code false}
     */
    @Override
    public boolean isAnnotatedWith(Class<?> annotatedClass) {
        return this.annotations.containsKey(annotatedClass);
    }

    /**
     * Determines if this bean is a singleton or non-bean lifecycle.
     *
     * @return {@code true} if the bean's lifecycle is SINGLETON or NON_BEAN, otherwise {@code false}
     */
    @Override
    public boolean isSingleton() {
        return getScope() == BeanScope.SINGLETON || getScope() == BeanScope.NON_BEAN;
    }

    /**
     * Determines if this bean is a prototype (not a singleton).
     *
     * @return {@code true} if the bean is prototype-scoped, otherwise {@code false}
     */
    @Override
    public boolean isPrototype() {
        return !isSingleton();
    }

    /**
     * Returns a string representation of this bean definition, including
     * basic information such as name, lifecycle scope, bean class, instance,
     * dependencies, parent, creation type, and instantiation strategy.
     *
     * @return a string describing the bean definition
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("[").append(name).append(':').append(scope).append(':').append(getInstantiationType()).append(']');
        builder.append(' ');
        builder.append("[").append(type).append("]");

        if (parent != null) {
            builder.append(" <- [").append(parent).append("]; ");
        }

        return builder.toString();
    }
}
