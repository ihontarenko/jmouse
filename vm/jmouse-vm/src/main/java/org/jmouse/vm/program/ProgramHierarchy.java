package org.jmouse.vm.program;

import org.jmouse.java.model.symbol.JavaTypeKey;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Hierarchy queries over authoritative declarations; never consults host reflection. */
public final class ProgramHierarchy {

    private final ProgramDeclarationSnapshot declarations;

    public ProgramHierarchy(ProgramDeclarationSnapshot declarations) {
        this.declarations = Objects.requireNonNull(declarations, "declarations");
    }

    public ProgramDeclarationSnapshot declarations() {
        return declarations;
    }

    /** Receiver first, followed by successive superclasses. Interface roots have no superclass. */
    public List<ProgramClassDeclaration> classChain(JavaTypeKey type) {
        List<ProgramClassDeclaration> chain = new ArrayList<>();
        Set<JavaTypeKey> seen = new HashSet<>();
        ProgramClassDeclaration current = declarations.requireType(type);
        while (true) {
            if (!seen.add(current.type())) {
                throw new IllegalArgumentException(
                        "Cyclic superclass hierarchy: " + current.type());
            }
            chain.add(current);
            if (current.superclass().isEmpty()) {
                break;
            }
            if (current.kind() == ProgramClassKind.INTERFACE) {
                throw new IllegalArgumentException(
                        "An interface cannot have a class superclass: " + current.type());
            }
            current = declarations.requireType(current.superclass().orElseThrow());
            if (current.kind() != ProgramClassKind.CLASS) {
                throw new IllegalArgumentException("Superclass is an interface: " + current.type());
            }
        }
        return List.copyOf(chain);
    }

    public Set<JavaTypeKey> allInterfaces(JavaTypeKey type) {
        Set<JavaTypeKey> interfaces = new LinkedHashSet<>();
        Map<JavaTypeKey, VisitState> states = new HashMap<>();
        for (ProgramClassDeclaration owner : classChain(type)) {
            for (JavaTypeKey direct : owner.interfaces()) {
                collectInterfaces(direct, states, interfaces);
            }
        }
        return Set.copyOf(interfaces);
    }

    public boolean isSubtype(JavaTypeKey subtype, JavaTypeKey supertype) {
        declarations.requireType(supertype);
        List<ProgramClassDeclaration> chain = classChain(subtype);
        Set<JavaTypeKey> interfaces = allInterfaces(subtype);
        return chain.stream().anyMatch(declaration -> declaration.type().equals(supertype))
                || interfaces.contains(supertype);
    }

    private void collectInterfaces(
            JavaTypeKey root, Map<JavaTypeKey, VisitState> states, Set<JavaTypeKey> interfaces) {
        Deque<InterfaceVisit> pending = new ArrayDeque<>();
        pending.addLast(new InterfaceVisit(root, VisitState.ENTERING));
        while (!pending.isEmpty()) {
            InterfaceVisit visit = pending.removeLast();
            if (visit.state() == VisitState.COMPLETE) {
                states.put(visit.type(), VisitState.COMPLETE);
                continue;
            }
            VisitState state = states.get(visit.type());
            if (state == VisitState.COMPLETE) {
                continue;
            }
            if (state == VisitState.ENTERING) {
                throw new IllegalArgumentException("Cyclic interface hierarchy: " + visit.type());
            }
            ProgramClassDeclaration declaration = declarations.requireType(visit.type());
            if (declaration.kind() != ProgramClassKind.INTERFACE
                    || declaration.superclass().isPresent()) {
                throw new IllegalArgumentException(
                        "Invalid superinterface declaration: " + visit.type());
            }
            interfaces.add(visit.type());
            states.put(visit.type(), VisitState.ENTERING);
            pending.addLast(new InterfaceVisit(visit.type(), VisitState.COMPLETE));
            for (JavaTypeKey parent : declaration.interfaces().reversed()) {
                pending.addLast(new InterfaceVisit(parent, VisitState.ENTERING));
            }
        }
    }

    private enum VisitState {
        ENTERING,
        COMPLETE
    }

    private record InterfaceVisit(JavaTypeKey type, VisitState state) {}
}
