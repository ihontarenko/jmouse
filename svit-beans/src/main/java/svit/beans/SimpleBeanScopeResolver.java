package svit.beans;

public class SimpleBeanScopeResolver implements ScopeResolver {

    /**
     * Resolves the {@link Scope} for a given bean name.
     *
     * @param name the name of the bean whose scope is to be resolved.
     * @return the {@link Scope} associated with the given bean name.
     */
    @Override
    public Scope resolveScope(String name) {
        return BeanScope.SINGLETON;
    }

}
