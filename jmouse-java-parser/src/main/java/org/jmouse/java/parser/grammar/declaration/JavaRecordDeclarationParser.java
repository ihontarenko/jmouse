package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.declaration.JavaRecordDeclarationNode;
import org.jmouse.java.parser.node.parameter.JavaParameterKind;

/** Parses a record declaration after its modifier prefix. */
public final class JavaRecordDeclarationParser implements ParseRule<JavaDeclarationHeader, JavaRecordDeclarationNode> {
    @Override
    public JavaRecordDeclarationNode parse(JavaDeclarationHeader header, ParseSession session) {
        JavaParsingInput input = header.input();
        input.expect(JavaKeyword.RECORD, session);
        JavaRecordDeclarationNode node = new JavaRecordDeclarationNode(input.name(), input.point());
        JavaTypeDeclarationParser.name(header, session, node);
        input.requireFeature(JavaSourceFeatures.RECORD_DECLARATIONS);
        JavaTypeDeclarationParser.parameters(input, session, node::addTypeParameter);
        input.expect("(", session);
        if (!input.take(")", session)) {
            do {
                JavaRecordComponentNode component = session.parse(JavaDeclarationRoles.RECORD_COMPONENT, input);
                node.addComponent(component);
                if (component.kind() == JavaParameterKind.VARIABLE_ARITY && input.matches(",")) {
                    throw input.error("A variable-arity record component must be last");
                }
            } while (input.take(",", session));
            input.expect(")", session);
        }
        JavaTypeDeclarationParser.relations(input, session, JavaKeyword.IMPLEMENTS, node::addInterfaceType);
        JavaTypeDeclarationParser.body(header, session, node, JavaTypeForm.RECORD);
        return node;
    }
}
