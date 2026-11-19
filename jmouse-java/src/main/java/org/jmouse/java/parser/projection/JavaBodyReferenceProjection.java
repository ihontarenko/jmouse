package org.jmouse.java.parser.projection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jmouse.java.parser.grammar.JavaOperatorCatalog;
import org.jmouse.java.parser.model.BodyReference;
import org.jmouse.java.parser.model.Expression;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.node.expression.JavaAssignmentOperator;
import org.jmouse.java.parser.node.expression.JavaUnaryOperator;

/** Reference roles over navigation records; this never reads or interprets source tokens. */
final class JavaBodyReferenceProjection {
    private record Pending(Expression expression, BodyReference.Role role) {
    }

    private final Set<String> writingOperators;
    private final String assignment;

    JavaBodyReferenceProjection() {
        JavaOperatorCatalog operators = JavaOperatorCatalog.standard();
        assignment = operators.require(JavaOperatorCatalog.Kind.ASSIGNMENT, JavaAssignmentOperator.ASSIGN.name()).spelling();
        writingOperators = operators.definitions().stream()
                .filter(definition -> definition.kind() == JavaOperatorCatalog.Kind.ASSIGNMENT
                        || definition.identity().equals(JavaUnaryOperator.INCREMENT.name())
                        || definition.identity().equals(JavaUnaryOperator.DECREMENT.name()))
                .map(JavaOperatorCatalog.Definition::spelling).collect(Collectors.toUnmodifiableSet());
    }

    List<BodyReference> references(List<Expression> expressions, SymbolId owner, SymbolId type) {
        List<BodyReference> references = new ArrayList<>();
        Deque<Pending> pending = new ArrayDeque<>();
        for (Expression expression : expressions) {
            pending.addLast(new Pending(expression, BodyReference.Role.READS));
        }
        while (!pending.isEmpty()) {
            Pending item = pending.removeFirst();
            Expression expression = item.expression();
            BodyReference.Role role = switch (expression.kind()) {
                case CALL -> BodyReference.Role.CALLS;
                case NEW -> BodyReference.Role.INSTANTIATES;
                case METHOD_REFERENCE -> BodyReference.Role.METHOD_REFERENCE;
                default -> item.role();
            };
            switch (expression.kind()) {
                case NAME, FIELD, CALL, NEW, METHOD_REFERENCE -> references.add(new BodyReference(expression, role, owner, type));
                default -> {
                }
            }
            if (expression.receiver() != null) {
                pending.addLast(new Pending(expression.receiver(), BodyReference.Role.READS));
            }
            for (int index = 0; index < expression.arguments().size(); index++) {
                BodyReference.Role childRole = BodyReference.Role.READS;
                if (expression.kind() == Expression.Kind.OPERATION && index == 0) {
                    if (expression.name().equals(assignment)) {
                        childRole = BodyReference.Role.WRITES;
                    } else if (writingOperators.contains(expression.name())) {
                        childRole = BodyReference.Role.READS_WRITES;
                    }
                }
                pending.addLast(new Pending(expression.arguments().get(index), childRole));
            }
        }
        references.sort(Comparator.comparingInt(reference -> reference.span().start().offset()));
        return List.copyOf(references);
    }
}
