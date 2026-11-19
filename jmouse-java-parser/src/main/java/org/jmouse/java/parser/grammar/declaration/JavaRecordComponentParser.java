package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.node.parameter.JavaParameterKind;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaVariableArityParameterNode;
import org.jmouse.java.parser.node.name.JavaModifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;

public final class JavaRecordComponentParser implements ParseRule<JavaParsingInput, JavaRecordComponentNode> {
    @Override
    public JavaRecordComponentNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaParameterNode parameter = session.parse(JavaParseRoles.PARAMETER, input);
        JavaModifiersNode modifiers = parameter instanceof JavaFormalParameterNode formal
                ? formal.modifiers() : ((JavaVariableArityParameterNode) parameter).modifiers();
        if (modifiers.entries().stream().anyMatch(JavaModifierNode.class::isInstance)) {
            throw input.errorAt("Record components admit annotations, not keyword modifiers", modifiers.span());
        }
        JavaRecordComponentNode node = new JavaRecordComponentNode(input.name(), parameter.span());
        if (parameter instanceof JavaFormalParameterNode formal) {
            if (!formal.dimensions().isEmpty()) {
                throw input.error("Record component dimensions belong to the type");
            }
            node.setKind(JavaParameterKind.FIXED);
            node.setModifiers(formal.removeChild(JavaFormalParameterNode.MODIFIERS, 0));
            node.setType(formal.removeChild(JavaFormalParameterNode.TYPE, 0));
            node.setName(formal.removeChild(JavaFormalParameterNode.NAME, 0));
        } else if (parameter instanceof JavaVariableArityParameterNode variable) {
            node.setKind(JavaParameterKind.VARIABLE_ARITY);
            node.setModifiers(variable.removeChild(JavaVariableArityParameterNode.MODIFIERS, 0));
            node.setType(variable.removeChild(JavaVariableArityParameterNode.TYPE, 0));
            while (!variable.ellipsisAnnotations().isEmpty()) {
                node.addEllipsisAnnotation(variable.removeChild(JavaVariableArityParameterNode.ELLIPSIS_ANNOTATIONS, 0));
            }
            node.setName(variable.removeChild(JavaVariableArityParameterNode.NAME, 0));
        } else {
            throw input.error("Expected a record component declaration");
        }
        return input.finish(node, start);
    }
}
