package org.jmouse.java.parser.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.model.Parameter;
import org.jmouse.java.parser.model.TypeReferenceRole;
import org.jmouse.java.parser.node.parameter.*;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

final class JavaParameterProjection {
    private JavaParameterProjection() {
    }

    static List<Parameter> parameters(JavaProjectionContext context, List<JavaParameterNode> nodes,
                                      JavaReceiverParameterNode receiver) {
        List<Parameter> parameters = new ArrayList<>();
        if (receiver != null) {
            parameters.add(new Parameter(JavaKeyword.THIS.spelling(),
                    context.type(receiver.type(), TypeReferenceRole.PARAMETER_TYPE),
                    context.annotations(receiver.annotations()), Set.of(), false, true, receiver.span()));
        }
        for (JavaParameterNode node : nodes) {
            if (node instanceof JavaFormalParameterNode formal) {
                String type = context.type(formal.type(), TypeReferenceRole.PARAMETER_TYPE)
                        + context.spelling.dimensions(formal.dimensions());
                parameters.add(new Parameter(formal.name().name(), type, context.annotations(formal.modifiers()),
                        context.modifiers(formal.modifiers()), false, false, formal.span()));
            } else if (node instanceof JavaVariableArityParameterNode variable) {
                variable.ellipsisAnnotations().forEach(context.references::annotation);
                parameters.add(new Parameter(variable.name().name(),
                        context.type(variable.type(), TypeReferenceRole.PARAMETER_TYPE),
                        context.annotations(variable.modifiers()), context.modifiers(variable.modifiers()),
                        true, false, variable.span()));
            } else {
                throw new IllegalArgumentException("An executable declaration requires written parameters");
            }
        }
        return List.copyOf(parameters);
    }

    static List<String> identities(List<JavaParameterNode> nodes) {
        return nodes.stream().map(node -> switch (node) {
            case JavaFormalParameterNode formal ->
                    JavaSourceSpelling.identity(formal.type()) + "[]".repeat(formal.dimensions().size());
            case JavaVariableArityParameterNode variable -> JavaSourceSpelling.identity(variable.type()) + "...";
            default -> throw new IllegalArgumentException("Cannot derive a declaration parameter identity");
        }).toList();
    }
}
