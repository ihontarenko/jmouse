package svit.container.instantiation;

import svit.container.BeanContext;
import svit.container.definition.BeanDependency;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractBeanInstantiationStrategy implements BeanInstantiationStrategy {

    protected Object[] resolveDependencies(List<BeanDependency> dependencies, BeanContext context) {
        List<Object> arguments = new ArrayList<>();

        for (BeanDependency dependency : dependencies) {
            arguments.add(context.getBean(dependency.type(), dependency.name()));
        }

        return arguments.toArray(Object[]::new);
    }

}
