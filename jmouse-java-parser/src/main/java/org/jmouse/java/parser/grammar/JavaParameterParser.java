package org.jmouse.java.parser.grammar;

import java.util.List;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaVariableArityParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.node.type.JavaInferredTypeNode;
import org.jmouse.java.parser.node.type.JavaVoidTypeNode;

/** A written parameter, including annotations on an ellipsis and post-name dimensions. */
public final class JavaParameterParser implements ParseRule<JavaParsingInput, JavaParameterNode> {
    @Override
    public JavaParameterNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaModifiersNode modifiers = session.parse(JavaParseRoles.VARIABLE_MODIFIERS, input);
        JavaTypeSyntaxNode type = session.parse(JavaParseRoles.TYPE, input);
        if (type instanceof JavaVoidTypeNode || type instanceof JavaInferredTypeNode
                && session.context(JavaGrammarContext.PARAMETER_FORM) != JavaGrammarContext.ParameterForm.LAMBDA) {
            throw input.errorAt("This parameter requires a written non-void type", type.span());
        }
        List<JavaAnnotationNode> ellipsisAnnotations = JavaTypeParser.annotations(input, session);
        if (input.take("...", session)) {
            if (type instanceof JavaInferredTypeNode) {
                throw input.errorAt("A variable-arity parameter requires a written type", type.span());
            }
            JavaVariableArityParameterNode node = new JavaVariableArityParameterNode(input.name(), input.point());
            node.setModifiers(modifiers);
            node.setType(type);
            ellipsisAnnotations.forEach(node::addEllipsisAnnotation);
            node.setName(input.identifier(session));
            return input.finish(node, start);
        }
        if (!ellipsisAnnotations.isEmpty()) {
            throw input.error("Expected an ellipsis after parameter annotations");
        }
        JavaFormalParameterNode node = new JavaFormalParameterNode(input.name(), input.point());
        node.setModifiers(modifiers);
        node.setType(type);
        node.setName(input.identifier(session));
        JavaTypeParser.dimensions(input, session).forEach(node::addDimension);
        return input.finish(node, start);
    }
}
