package org.jmouse.beans.definition;

import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.Scope;
import org.jmouse.beans.instantiation.BeanInstantiationStrategy;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * An abstract base class for {@link BeanDefinition} implementations.
 * Manages common structured descriptor such as dependencies, lifecycle, annotations, and more.
 * <p>
 * This class serves as a foundation for concrete structured definitions in a container,
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
     * A list of dependencies that this structured requires.
     */
    protected final List<BeanDependency> dependencies;

    /**
     * A map of annotations (by their types) present on this structured definition.
     */
    protected final Map<Class<? extends Annotation>, Annotation> annotations;

    /**
     * The unique name of the structured.
     */
    protected String name;

    /**
     * The type (class) of the structured.
     */
    protected Class<?> type;

    protected List<Class<?>> parametrizedTypes;

    /**
     * Indicates whether the structured is proxied.
     */
    protected boolean proxied = false;

    /**
     * The parent structured definition, if any.
     */
    protected BeanDefinition parent;

    /**
     * Child structured definitions that extend or complement this structured's definition.
     */
    protected List<BeanDefinition> children;

    /**
     * The lifecycle scope for this structured (e.g., SINGLETON, PROTOTYPE).
     */
    protected Scope scope;

    /**
     * The actual structured instance, if created.
     */
    protected Object instance;

    /**
     * The strategy for instantiating the structured.
     */
    protected BeanInstantiationStrategy strategy;

    /**
     * Constructs a new {@code AbstractBeanDefinition} with the specified name and type.
     *
     * @param name the unique name of the structured
     * @param type the structured's class
     */
    public AbstractBeanDefinition(String name, Class<?> type) {
        this.name = name;
        this.type = type;
        this.dependencies = new ArrayList<>();
        this.children = new ArrayList<>();
        this.annotations = new HashMap<>();
    }

    /**
     * Retrieves the name of this structured.
     *
     * @return the name of the structured
     */
    @Override
    public String getBeanName() {
        return name;
    }

    /**
     * Sets the name of this structured.
     *
     * @param name the new structured name
     */
    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the class of this structured.
     *
     * @return the structured's class
     */
    @Override
    public Class<?> getBeanClass() {
        return type;
    }

    /**
     * Sets the class of this structured.
     *
     * @param type the new class of the structured
     */
    @Override
    public void setBeanClass(Class<?> type) {
        this.type = type;
    }

    /**
     * Returns a list of parameterized types associated with this structured definition.
     * Useful when the structured class is a generic type, and its parameters need to be
     * tracked for reflection or dependency injection purposes.
     *
     * @return a list of parameterized types
     */
    @Override
    public List<Class<?>> getParametrizedTypes() {
        return List.copyOf(parametrizedTypes);
    }

    /**
     * Sets the list of parameterized types for this structured definition. This is
     * particularly helpful when dealing with generic types, allowing you to
     * specify or update the actual type parameters used by the structured.
     *
     * @param types a list of parameterized types
     */
    @Override
    public void setParametrizedTypes(List<Class<?>> types) {
        this.parametrizedTypes = List.copyOf(types);
    }

    /**
     * Indicates whether the structured represented by this definition is proxied.
     *
     * @return {@code true} if the structured is proxied, otherwise {@code false}.
     */
    @Override
    public boolean isProxied() {
        return proxied;
    }

    /**
     * Sets whether the structured represented by this definition is proxied.
     *
     * @param proxied {@code true} if the structured should be proxied, {@code false} otherwise.
     */
    @Override
    public void setProxied(boolean proxied) {
        this.proxied = proxied;
    }

    /**
     * Retrieves the creation type for this structured, indicating how it should be instantiated.
     *
     * @return a {@link BeanInstantiationType} value.
     */
    @Override
    public BeanInstantiationType getInstantiationType() {
        return null;
    }

    /**
     * Retrieves the parent definition of this structured, if any.
     *
     * @return the parent structured definition, or {@code null} if none
     */
    @Override
    public BeanDefinition getParentDefinition() {
        return parent;
    }

    /**
     * Sets the parent definition for this structured.
     *
     * @param parent the new parent structured definition
     */
    @Override
    public void setParentDefinition(BeanDefinition parent) {
        this.parent = parent;
    }

    /**
     * Retrieves the child definitions of this structured.
     *
     * @return a list of child definitions
     */
    @Override
    public List<BeanDefinition> getChildrenDefinitions() {
        return children;
    }

    /**
     * Adds a child definition to this structured.
     *
     * @param child the child definition to add
     */
    @Override
    public void addChildDefinition(BeanDefinition child) {
        children.add(child);
    }

    /**
     * Retrieves the lifecycle scope of this structured (e.g., SINGLETON, PROTOTYPE).
     *
     * @return the lifecycle scope
     */
    @Override
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the lifecycle scope of this structured.
     *
     * @param scope the new lifecycle scope
     */
    @Override
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * Retrieves the dependencies required by this structured.
     *
     * @return a list of structured dependencies
     */
    @Override
    public List<BeanDependency> getBeanDependencies() {
        return dependencies;
    }

    /**
     * Retrieves the actual instance of this structured, if it has been created.
     *
     * @param <T> the expected type of the structured
     * @return the structured instance, or {@code null} if not created
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBeanInstance() {
        return (T) instance;
    }

    /**
     * Sets the actual instance of this structured.
     *
     * @param instance the new structured instance
     */
    @Override
    public void setBeanInstance(Object instance) {
        this.instance = instance;
    }

    /**
     * Retrieves the instantiation strategy for this structured (e.g., constructor, method, structured factory).
     *
     * @return the structured instantiation strategy
     */
    @Override
    public BeanInstantiationStrategy getBeanCreationStrategy() {
        return strategy;
    }

    /**
     * Sets the instantiation strategy for this structured.
     *
     * @param strategy the new structured instantiation strategy
     */
    @Override
    public void setBeanCreationStrategy(BeanInstantiationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Retrieves the collection of annotations present on this structured definition.
     *
     * @return a collection of annotations
     */
    @Override
    public Collection<Annotation> getAnnotations() {
        return annotations.values();
    }

    /**
     * Retrieves a specific annotation of the given type from this structured definition.
     *
     * @param <A> the type of the annotation to retrieve
     * @param annotationType the class structured of the annotation type
     * @return the annotation instance if present, otherwise {@code null}
     */
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return (A) annotations.get(annotationType);
    }

    /**
     * Sets the annotations present on this structured definition.
     *
     * @param annotations a collection of annotations to associate with this structured
     */
    @Override
    public void setAnnotations(Collection<Annotation> annotations) {
        this.annotations.clear();
        for (Annotation annotation : annotations) {
            this.annotations.put(annotation.annotationType(), annotation);
        }
    }

    /**
     * Determines if this structured definition is annotated with the specified class.
     *
     * @param annotatedClass the annotation class to check
     * @return {@code true} if this structured is annotated with {@code annotatedClass}, otherwise {@code false}
     */
    @Override
    public boolean isAnnotatedWith(Class<?> annotatedClass) {
        return this.annotations.containsKey(annotatedClass);
    }

    /**
     * Determines if this structured is a singleton or non-structured lifecycle.
     *
     * @return {@code true} if the structured's lifecycle is SINGLETON or NON_BEAN, otherwise {@code false}
     */
    @Override
    public boolean isSingleton() {
        return getScope() == BeanScope.SINGLETON || getScope() == BeanScope.NON_BEAN;
    }

    /**
     * Determines if this structured is a prototype (not a singleton).
     *
     * @return {@code true} if the structured is prototype-scoped, otherwise {@code false}
     */
    @Override
    public boolean isPrototype() {
        return !isSingleton();
    }

    /**
     * Returns a string representation of this structured definition, including
     * basic information such as name, lifecycle scope, structured class, instance,
     * dependencies, parent, creation type, and instantiation strategy.
     *
     * @return a string describing the structured definition
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
