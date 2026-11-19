package org.jmouse.java.parser.grammar.declaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaGrammarContext;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.declaration.JavaDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaEmptyMemberDeclarationNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

/** Reads one common member prefix, then delegates ownership to the selected production. */
public final class JavaMemberParser implements ParseRule<JavaParsingInput, JavaDeclarationNode> {
    private final Set<String> typeStarters;

    public JavaMemberParser(Set<String> typeStarters) {
        this.typeStarters = Set.copyOf(typeStarters);
    }

    @Override
    public JavaDeclarationNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        if (input.take(";", session)) {
            return new JavaEmptyMemberDeclarationNode(input.name(), input.spanFrom(start));
        }
        JavaModifiersNode modifiers = session.parse(JavaParseRoles.MODIFIERS, input);
        JavaDeclarationHeader declaration = new JavaDeclarationHeader(input, modifiers, start);
        if (typeStarters.contains(input.token().value())) {
            return session.parse(JavaDeclarationRoles.TYPE_HEADER, declaration);
        }
        if (input.matches("{")) {
            return session.parse(JavaDeclarationRoles.INITIALIZER, declaration);
        }
        List<JavaTypeParameterNode> typeParameters = new ArrayList<>();
        JavaTypeDeclarationParser.parameters(input, session, typeParameters::add);
        JavaEnclosingType enclosing = session.context(JavaGrammarContext.ENCLOSING_TYPE);
        if (enclosing != null && input.token().value().equals(enclosing.name())
                && (input.cursor().lookAt(1).value().equals("(") || input.cursor().lookAt(1).value().equals("{"))) {
            JavaConstructorHeader constructor = new JavaConstructorHeader(declaration, typeParameters, input.identifier(session));
            if (input.matches("{")) {
                if (enclosing.form() != JavaTypeForm.RECORD) {
                    throw input.error("A compact constructor requires an enclosing record");
                }
                return session.parse(JavaDeclarationRoles.COMPACT_CONSTRUCTOR, constructor);
            }
            return session.parse(JavaDeclarationRoles.CONSTRUCTOR, constructor);
        }
        JavaTypeSyntaxNode type = session.parse(JavaParseRoles.TYPE, input);
        JavaIdentifierNode name = input.identifier(session);
        JavaMemberHeader header = new JavaMemberHeader(declaration, typeParameters, type, name);
        if (input.matches("(")) {
            return enclosing != null && enclosing.form() == JavaTypeForm.ANNOTATION
                    ? session.parse(JavaDeclarationRoles.ANNOTATION_ELEMENT, header)
                    : session.parse(JavaDeclarationRoles.METHOD, header);
        }
        return session.parse(JavaDeclarationRoles.FIELD, header);
    }
}
