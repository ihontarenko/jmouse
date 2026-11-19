package org.jmouse.vm.graph;

import org.jmouse.vm.graph.dependency.ClassInitializationDependencies;
import org.jmouse.vm.graph.dependency.FieldUsageDependencies;
import org.jmouse.vm.graph.dependency.MethodImplementationDependencies;
import org.jmouse.vm.graph.dependency.TypeInstantiationDependencies;
import org.jmouse.vm.graph.dependency.TypeReachabilityDependencies;
import org.jmouse.vm.graph.dependency.VirtualDispatchDependencies;
import org.jmouse.vm.graph.dependency.VirtualInvocationDependencies;
import org.jmouse.vm.program.ClassInitializationStrategy;
import org.jmouse.vm.program.ProgramHierarchy;
import org.jmouse.vm.program.VirtualDispatchStrategy;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Immutable composition. Individual contributors may be replaced or extended without core edits.
 */
public final class ProgramDependencies implements DependencyProvider {
    private final List<DependencyProvider> providers;

    public ProgramDependencies(
            ProgramHierarchy hierarchy,
            VirtualDispatchStrategy dispatch,
            ClassInitializationStrategy initialization,
            ReceiverTypeStrategy receivers,
            MethodDependencyProvider bodies) {
        this(
                List.of(
                        new TypeReachabilityDependencies(hierarchy),
                        new TypeInstantiationDependencies(hierarchy),
                        new MethodImplementationDependencies(hierarchy, bodies),
                        new FieldUsageDependencies(hierarchy),
                        new ClassInitializationDependencies(hierarchy, initialization),
                        new VirtualInvocationDependencies(hierarchy, receivers),
                        new VirtualDispatchDependencies(hierarchy, dispatch)));
    }

    public ProgramDependencies(List<DependencyProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public List<DependencyProvider> providers() {
        return providers;
    }

    @Override
    public Iterable<DependencyRule> dependencies(ProgramRequirement requirement) {
        return () ->
                providers.stream()
                        .flatMap(
                                provider ->
                                        StreamSupport.stream(
                                                provider.dependencies(requirement).spliterator(),
                                                false))
                        .iterator();
    }
}
