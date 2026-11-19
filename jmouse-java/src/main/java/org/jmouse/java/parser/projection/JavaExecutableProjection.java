package org.jmouse.java.parser.projection;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.TraversalAction;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.el.translate.Translator;
import org.jmouse.java.parser.JavaSyntaxResult;
import org.jmouse.java.parser.grammar.JavaParsingMode;
import org.jmouse.java.parser.model.ConstructorDeclaration;
import org.jmouse.java.parser.model.EnumConstantDeclaration;
import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.FieldDeclaration;
import org.jmouse.java.parser.model.InitializerDeclaration;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.LocalTypeDeclaration;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.node.body.JavaBlockBodyNode;
import org.jmouse.java.parser.node.declaration.JavaCompactConstructorDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaConstructorDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaEnumConstantNode;
import org.jmouse.java.parser.node.declaration.JavaInitializerDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaMethodDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaRecordDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaVariableDeclaratorNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Navigation metadata from a single full AST. No lexer or parsing algorithm belongs to this view.
 */
public final class JavaExecutableProjection implements Translator<JavaSourceFile> {
    private static final Capabilities CAPABILITIES =
            Capabilities.of(
                    JavaExecutableProjection.class.getName(),
                    JavaProjectionCapabilities.DECLARATION_METADATA,
                    JavaProjectionCapabilities.EXECUTABLE_METADATA);
    private final JavaSyntaxResult<JavaCompilationUnitNode> syntax;

    public JavaExecutableProjection(JavaSyntaxResult<JavaCompilationUnitNode> syntax) {
        this.syntax = Objects.requireNonNull(syntax, "syntax");
        if (syntax.mode() != JavaParsingMode.COMPLETE || !syntax.omittedRegions().isEmpty()) {
            throw new TranslationRefusedException(
                    "Executable metadata requires all executable regions to be read");
        }
    }

    public static JavaSourceFile project(JavaSyntaxResult<JavaCompilationUnitNode> syntax) {
        return new JavaExecutableProjection(syntax).translate(syntax.root());
    }

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public JavaSourceFile translate(Node root, Bindings bindings) {
        if (root != syntax.root() || !bindings.isEmpty()) {
            throw new TranslationRefusedException(
                    "Executable metadata requires its source root and no runtime bindings");
        }
        JavaCompilationUnitNode unit = syntax.root();
        JavaProjectionContext context = new JavaProjectionContext(syntax.source(), unit);
        JavaSourceFile file =
                new JavaDeclarationProjection(syntax.source(), syntax.problems())
                        .translate(unit, context);
        JavaExecutableBodyProjection bodyProjection = new JavaExecutableBodyProjection(context);
        List<ExecutableBody> bodies = new ArrayList<>();
        List<LocalTypeDeclaration> localTypes = new ArrayList<>();
        unit.accept(
                new Visitor() {
                    @Override
                    public TraversalAction enter(Node node) {
                        JavaLocalTypeProjection.declare(context, node, localTypes);
                        if (node instanceof JavaMethodDeclarationNode method
                                && method.body() instanceof JavaBlockBodyNode body) {
                            MethodDeclaration declaration =
                                    (MethodDeclaration) context.declarations.get(method);
                            if (declaration == null) {
                                throw new TranslationRefusedException(
                                        "Nested methods require their own navigation ownership");
                            }
                            SymbolId type = enclosingType(context, method);
                            bodies.add(
                                    bodyProjection.read(
                                            body.block(),
                                            declaration.symbolId(),
                                            type,
                                            method.parameters(),
                                            body.span()));
                            return TraversalAction.CONTINUE;
                        }
                        if (node instanceof JavaConstructorDeclarationNode constructor) {
                            ConstructorDeclaration declaration =
                                    (ConstructorDeclaration) context.declarations.get(constructor);
                            if (declaration == null) {
                                throw new TranslationRefusedException(
                                        "Nested constructors require their own navigation"
                                                + " ownership");
                            }
                            bodies.add(
                                    bodyProjection.read(
                                            constructor.body(),
                                            declaration.symbolId(),
                                            enclosingType(context, constructor),
                                            constructor.parameters(),
                                            constructor.body().span()));
                            return TraversalAction.CONTINUE;
                        }
                        if (node instanceof JavaCompactConstructorDeclarationNode constructor) {
                            ConstructorDeclaration declaration =
                                    (ConstructorDeclaration) context.declarations.get(constructor);
                            JavaRecordDeclarationNode record = enclosingRecord(constructor);
                            bodies.add(
                                    bodyProjection.read(
                                            constructor.body(),
                                            declaration.symbolId(),
                                            enclosingType(context, constructor),
                                            record.components(),
                                            constructor.body().span()));
                            return TraversalAction.CONTINUE;
                        }
                        if (node instanceof JavaEnumConstantNode constant
                                && !constant.arguments().isEmpty()) {
                            EnumConstantDeclaration declaration =
                                    (EnumConstantDeclaration) context.declarations.get(constant);
                            bodies.add(
                                    bodyProjection.arguments(
                                            constant.arguments(),
                                            declaration.symbolId(),
                                            enclosingType(context, constant),
                                            declaration.signatureSpan()));
                            return TraversalAction.CONTINUE;
                        }
                        if (node instanceof JavaInitializerDeclarationNode initializer) {
                            InitializerDeclaration declaration =
                                    (InitializerDeclaration) context.declarations.get(initializer);
                            if (declaration == null) {
                                throw new TranslationRefusedException(
                                        "Nested initializers require their own navigation"
                                                + " ownership");
                            }
                            bodies.add(
                                    bodyProjection.read(
                                            initializer.body(),
                                            declaration.symbolId(),
                                            enclosingType(context, initializer),
                                            List.of(),
                                            initializer.body().span()));
                            return TraversalAction.CONTINUE;
                        }
                        if (node instanceof JavaVariableDeclaratorNode variable
                                && context.declarations.get(variable)
                                        instanceof FieldDeclaration declaration
                                && variable.initializer().isPresent()) {
                            bodies.add(
                                    bodyProjection.read(
                                            variable.initializer().get(),
                                            declaration.symbolId(),
                                            enclosingType(context, variable),
                                            List.of(),
                                            declaration.variableSpan()));
                            return TraversalAction.CONTINUE;
                        }
                        return TraversalAction.CONTINUE;
                    }
                });
        return new JavaSourceFile(
                file.sourceName(),
                file.packageDeclaration(),
                file.moduleDeclaration(),
                file.imports(),
                file.types(),
                file.comments(),
                context.documentation.documents(),
                file.problems(),
                context.references.references(),
                bodies,
                localTypes,
                syntax.coverage());
    }

    private static SymbolId enclosingType(JavaProjectionContext context, Node node) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (context.declarations.get(parent) instanceof TypeDeclaration type) {
                return type.symbolId();
            }
        }
        throw new TranslationRefusedException(
                "An executable declaration requires an enclosing type");
    }

    private static JavaRecordDeclarationNode enclosingRecord(Node node) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof JavaRecordDeclarationNode record) {
                return record;
            }
        }
        throw new TranslationRefusedException(
                "A compact constructor requires its record declaration");
    }
}
