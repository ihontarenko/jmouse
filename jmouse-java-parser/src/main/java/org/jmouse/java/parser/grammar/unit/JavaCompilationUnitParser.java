package org.jmouse.java.parser.grammar.unit;

import java.util.Optional;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.el.parser.ParsingLimitException;
import org.jmouse.java.parser.grammar.declaration.JavaDeclarationRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaRecoveryBoundaries;
import org.jmouse.java.parser.grammar.JavaSyntaxRecovery;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.grammar.module.JavaModuleRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.declaration.JavaDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaEmptyTypeDeclarationNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.module.JavaModuleDeclarationNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.jmouse.java.parser.node.unit.JavaImportNode;
import org.jmouse.java.parser.node.unit.JavaPackageDeclarationNode;
import org.jmouse.java.parser.source.ParseProblem;

/** One compilation unit, with no opaque executable regions in this full-syntax entry. */
public final class JavaCompilationUnitParser implements ParseRule<JavaParsingInput, JavaCompilationUnitNode> {
    @Override
    public JavaCompilationUnitNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaCompilationUnitNode node = new JavaCompilationUnitNode(input.name(), input.point());
        while (!input.atEnd()) {
            Optional<JavaSyntaxNode> item;
            try {
                item = JavaSyntaxRecovery.attempt(input, session,
                        JavaRecoveryBoundaries.TOP_LEVEL, ParseProblem.Phase.STRUCTURE, () -> item(input, session, node));
            } catch (ParsingLimitException exception) {
                input.reportLimit(exception, input.point(), ParseProblem.Phase.STRUCTURE);
                break;
            }
            if (item.isPresent()) {
                JavaSyntaxNode syntax = item.get();
                if (syntax instanceof JavaPackageDeclarationNode declaration) {
                    node.setPackageDeclaration(declaration);
                } else if (syntax instanceof JavaImportNode declaration) {
                    node.addImport(declaration);
                } else if (syntax instanceof JavaModuleDeclarationNode declaration) {
                    node.setModule(declaration);
                } else if (syntax instanceof JavaDeclarationNode declaration) {
                    node.addDeclaration(declaration);
                } else {
                    throw new IllegalStateException("Unsupported compilation-unit production: " + syntax.schema().kind().name());
                }
            }
        }
        return input.finish(node, start);
    }

    private JavaSyntaxNode item(JavaParsingInput input, ParseSession session, JavaCompilationUnitNode unit) {
        if (unit.module().isPresent()) {
            throw input.error("Unexpected syntax after a module declaration");
        }
        int start = input.offset();
        if (input.take(JavaToken.SEMICOLON, session)) {
            return new JavaEmptyTypeDeclarationNode(input.name(), input.spanFrom(start));
        }
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        JavaKeyword declaration;
        try {
            JavaTypeParser.annotations(input, session);
            declaration = JavaKeyword.fromSpelling(input.token().value()).orElse(null);
        } finally {
            input.restore(checkpoint);
        }
        if (declaration == JavaKeyword.PACKAGE) {
            if (unit.packageDeclaration().isPresent() || !unit.imports().isEmpty() || !unit.declarations().isEmpty()) {
                throw input.error("A package declaration must precede imports and types");
            }
            return session.parse(JavaUnitRoles.PACKAGE, input);
        }
        if (declaration == JavaKeyword.IMPORT) {
            if (!unit.declarations().isEmpty()) {
                throw input.error("Imports must precede type declarations");
            }
            return session.parse(JavaUnitRoles.IMPORT, input);
        }
        if (declaration == JavaKeyword.OPEN || declaration == JavaKeyword.MODULE) {
            if (unit.packageDeclaration().isPresent() || !unit.declarations().isEmpty()) {
                throw input.error("A modular compilation unit cannot declare a package or types");
            }
            return session.parse(JavaModuleRoles.MODULE, input);
        }
        return session.parse(JavaDeclarationRoles.TYPE_DECLARATION, input);
    }
}
