package org.jmouse.vm.graph.dependency;

import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.vm.graph.DependencyReason;
import org.jmouse.vm.graph.DependencyRule;
import org.jmouse.vm.graph.ProgramRequirement;
import org.jmouse.vm.graph.StandardDependencyKind;
import org.jmouse.vm.program.ProgramHierarchy;
import org.jmouse.vm.program.ProgramOrigin;

import java.util.List;
import java.util.Objects;
import java.util.Set;

record DependencyOrigin(ProgramOrigin location) {
    DependencyOrigin {
        Objects.requireNonNull(location, "location");
    }

    static DependencyOrigin forType(ProgramHierarchy hierarchy, JavaTypeKey type) {
        return new DependencyOrigin(hierarchy.declarations().requireType(type).origin().location());
    }

    void add(
            List<DependencyRule> rules,
            ProgramRequirement source,
            ProgramRequirement required,
            StandardDependencyKind kind) {
        rules.add(rule(source, required, kind));
    }

    DependencyRule rule(
            ProgramRequirement source, ProgramRequirement required, StandardDependencyKind kind) {
        return new DependencyRule(Set.of(source), required, reason(kind, source));
    }

    DependencyReason reason(StandardDependencyKind kind, ProgramRequirement source) {
        return new DependencyReason(kind, kind.name() + ": " + source, List.of(location));
    }
}
