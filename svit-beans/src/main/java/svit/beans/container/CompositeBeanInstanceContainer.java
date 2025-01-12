package svit.beans.container;

import svit.beans.BeanInstanceContainer;
import svit.beans.Scope;
import svit.beans.ScopeResolver;
import svit.beans.SimpleBeanScopeResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompositeBeanInstanceContainer implements BeanInstanceContainer {

    /**
     * A mapping of {@link Scope} to their respective {@link BeanInstanceContainer}.
     * <p>
     * This map is used to dynamically associate scopes with their corresponding
     * containers, allowing for flexible management of bean instances based on scope.
     */
    private final Map<Scope, BeanInstanceContainer> containers = new ConcurrentHashMap<>();

    private final ScopeResolver scopeResolver = new SimpleBeanScopeResolver();

    /**
     * Retrieves a bean instance by its name.
     *
     * @param name the name of the bean to retrieve.
     * @return the bean instance, or {@code null} if no bean is found with the given name.
     */
    @Override
    public <T> T getBean(String name) {
        Scope scope = scopeResolver.resolveScope(name);
        T     bean  = containers.get(scope).getBean(name);

        if (bean == null) {

        }

        return bean;
    }

    /**
     * Registers a bean instance with the given name.
     *
     * @param name the name of the bean.
     * @param bean the bean instance to register.
     */
    @Override
    public void registerBean(String name, Object bean) {
        Scope scope = scopeResolver.resolveScope(name);
        containers.get(scope).registerBean(name, bean);
    }

    /**
     * Checks whether a bean with the specified name is already registered in this container.
     *
     * @param name the name of the bean.
     * @return {@code true} if a bean with the given name exists, otherwise {@code false}.
     */
    @Override
    public boolean containsBean(String name) {
        Scope scope = scopeResolver.resolveScope(name);
        return containers.get(scope).containsBean(name);
    }

    /**
     * Determines if the current implementation supports the given {@link Scope}.
     * <p>
     * By default, this method returns {@code false}, indicating that the scope
     * is not supported. Implementations can override this method to specify
     * which scopes they support.
     *
     * @param scope the {@link Scope} to check for support.
     * @return {@code true} if the scope is supported, {@code false} otherwise.
     */
    @Override
    public boolean supports(Scope scope) {
        return containers.containsKey(scope);
    }

}
