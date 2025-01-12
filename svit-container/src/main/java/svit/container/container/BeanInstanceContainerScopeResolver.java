package svit.container.container;

import svit.container.Scope;

@FunctionalInterface
public interface BeanInstanceContainerScopeResolver {
    Scope resolve();
}
