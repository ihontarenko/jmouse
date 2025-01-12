package svit.beans.container;

import svit.beans.Scope;

@FunctionalInterface
public interface BeanInstanceContainerScopeResolver {
    Scope resolve();
}
