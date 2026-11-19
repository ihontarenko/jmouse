package org.jmouse.vm.graph.dependency;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.DependencyProvider;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramDependencyException;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.ReceiverTypeStrategy;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.graph.requirement.TypeInstantiationRequirement;
import org.jmouse.vm.graph.requirement.TypeReachabilityRequirement;
import org.jmouse.vm.graph.requirement.VirtualDispatchRequirement;
import org.jmouse.vm.graph.requirement.VirtualInvocationRequirement;
import org.jmouse.vm.program.ProgramHierarchy;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class VirtualInvocationDependencies implements DependencyProvider {

    private final ProgramHierarchy hierarchy;
    private final ReceiverTypeStrategy receivers;

    public VirtualInvocationDependencies(
            ProgramHierarchy hierarchy, ReceiverTypeStrategy receivers) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        this.receivers = Objects.requireNonNull(receivers, "receivers");
    }

    @Override
    public Iterable<DependencyRule> dependencies(ProgramRequirement requirement) {
        if (!(requirement instanceof VirtualInvocationRequirement invocation)) {
            return List.of();
        }
        hierarchy
                .declarations()
                .findMethod(invocation.resolvedMethod())
                .orElseThrow(
                        () ->
                                new ProgramDependencyException(
                                        requirement, "Unresolved virtual invocation"));
        DependencyOrigin origin =
                DependencyOrigin.forType(hierarchy, invocation.resolvedMethod().owner());
        DependencyRule owner =
                origin.rule(
                        requirement,
                        new TypeReachabilityRequirement(invocation.resolvedMethod().owner()),
                        StandardDependencyKind.DECLARATION);
        return () -> Stream.concat(Stream.of(owner), dispatchRules(invocation, origin)).iterator();
    }

    private Stream<DependencyRule> dispatchRules(
            VirtualInvocationRequirement invocation, DependencyOrigin origin) {
        Iterable<JavaTypeKey> candidates = receivers.candidates(invocation.resolvedMethod());
        return StreamSupport.stream(candidates.spliterator(), false)
                .map(receiver -> dispatchRule(invocation, receiver, origin));
    }

    private DependencyRule dispatchRule(
            VirtualInvocationRequirement invocation,
            JavaTypeKey receiver,
            DependencyOrigin origin) {
        if (!hierarchy.isSubtype(receiver, invocation.resolvedMethod().owner())) {
            throw new ProgramDependencyException(
                    invocation, "Receiver policy supplied an unrelated type: " + receiver);
        }
        return new DependencyRule(
                Set.of(invocation, new TypeInstantiationRequirement(receiver)),
                new VirtualDispatchRequirement(invocation.resolvedMethod(), receiver),
                origin.reason(StandardDependencyKind.VIRTUAL_DISPATCH, invocation));
    }
}
