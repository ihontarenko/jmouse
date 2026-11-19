package org.jmouse.java.parser.grammar.declaration;

import java.util.function.Consumer;
import java.util.Map;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaGrammarContext;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

/** Named type entry and common header grammar; each type form has an addressable rule. */
public final class JavaTypeDeclarationParser implements ParseRule<JavaParsingInput, JavaTypeDeclarationNode> {
    @Override
    public JavaTypeDeclarationNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        return session.parse(JavaDeclarationRoles.TYPE_HEADER,
                new JavaDeclarationHeader(input, session.parse(JavaParseRoles.MODIFIERS, input), start));
    }

    public static void name(JavaDeclarationHeader header, ParseSession session, JavaTypeDeclarationNode node) {
        node.setModifiers(header.modifiers());
        node.setName(header.input().typeIdentifier(session));
    }

    public static void parameters(JavaParsingInput input, ParseSession session,
                                  Consumer<JavaTypeParameterNode> destination) {
        if (input.take("<", session)) {
            do {
                destination.accept(session.parse(JavaDeclarationRoles.TYPE_PARAMETER, input));
            } while (input.take(",", session));
            input.expect(">", session);
        }
    }

    public static void relations(JavaParsingInput input, ParseSession session, JavaKeyword keyword,
                                 Consumer<JavaTypeSyntaxNode> destination) {
        if (input.take(keyword, session)) {
            do {
                destination.accept(session.parse(JavaParseRoles.TYPE, input));
            } while (input.take(",", session));
        }
    }

    public static void body(JavaDeclarationHeader header, ParseSession session, JavaTypeDeclarationNode node,
                            JavaTypeForm form) {
        node.setBody(session.with(JavaGrammarContext.ENCLOSING_TYPE, new JavaEnclosingType(node.name().name(), form),
                () -> session.parse(JavaDeclarationRoles.TYPE_BODY, header.input())));
        header.input().finish(node, header.start());
    }

    public static final class HeaderParser implements ParseRule<JavaDeclarationHeader, JavaTypeDeclarationNode> {
        private final Map<String, ParseRole<JavaDeclarationHeader, ? extends JavaTypeDeclarationNode>> routes;

        public HeaderParser(Map<String, ParseRole<JavaDeclarationHeader, ? extends JavaTypeDeclarationNode>> routes) {
            this.routes = Map.copyOf(routes);
        }

        @Override
        public JavaTypeDeclarationNode parse(JavaDeclarationHeader header, ParseSession session) {
            ParseRole<JavaDeclarationHeader, ? extends JavaTypeDeclarationNode> role =
                    routes.get(header.input().token().value());
            if (role == null) {
                throw header.input().error("Expected a type declaration");
            }
            return session.parse(role, header);
        }
    }
}
