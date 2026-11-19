package org.jmouse.java.parser.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.TraversalAction;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.el.translate.Translator;
import org.jmouse.java.parser.JavaSyntaxResult;
import org.jmouse.java.parser.model.ConstructorDeclaration;
import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.FieldDeclaration;
import org.jmouse.java.parser.model.InitializerDeclaration;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.node.body.JavaBlockBodyNode;
import org.jmouse.java.parser.node.declaration.JavaConstructorDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaInitializerDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaMethodDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaVariableDeclaratorNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;

/** Navigation metadata from a single full AST. No lexer or parsing algorithm belongs to this view. */
public final class JavaExecutableProjection implements Translator<JavaSourceFile> {
    private static final Capabilities CAPABILITIES = Capabilities.of(JavaExecutableProjection.class.getName(),
            JavaProjectionCapabilities.DECLARATION_METADATA, JavaProjectionCapabilities.EXECUTABLE_METADATA);
    private final JavaSyntaxResult<JavaCompilationUnitNode> syntax;

    public JavaExecutableProjection(JavaSyntaxResult<JavaCompilationUnitNode> syntax) {
        this.syntax = Objects.requireNonNull(syntax, "syntax");
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
            throw new TranslationRefusedException("Executable metadata requires its source root and no runtime bindings");
        }
        JavaCompilationUnitNode unit = syntax.root();
        JavaProjectionContext context = new JavaProjectionContext(syntax.source(), unit);
        JavaSourceFile file = new JavaDeclarationProjection(syntax.source(), syntax.problems()).translate(unit, context);
        JavaExecutableBodyProjection bodyProjection = new JavaExecutableBodyProjection(context);
        List<ExecutableBody> bodies = new ArrayList<>();
        unit.accept(new Visitor() {
            @Override
            public TraversalAction enter(Node node) {
                if (node instanceof JavaMethodDeclarationNode method && method.body() instanceof JavaBlockBodyNode body) {
                    MethodDeclaration declaration = (MethodDeclaration) context.declarations.get(method);
                    if (declaration == null) {
                        throw new TranslationRefusedException("Nested methods require their own navigation ownership");
                    }
                    SymbolId type = enclosingType(context, method);
                    bodies.add(bodyProjection.read(body.block(), declaration.symbolId(), type,
                            method.parameters(), body.span()));
                    return TraversalAction.SKIP_CHILDREN;
                }
                if (node instanceof JavaConstructorDeclarationNode constructor) {
                    ConstructorDeclaration declaration = (ConstructorDeclaration) context.declarations.get(constructor);
                    if (declaration == null) {
                        throw new TranslationRefusedException("Nested constructors require their own navigation ownership");
                    }
                    bodies.add(bodyProjection.read(constructor.body(), declaration.symbolId(),
                            enclosingType(context, constructor), constructor.parameters(), constructor.body().span()));
                    return TraversalAction.SKIP_CHILDREN;
                }
                if (node instanceof JavaInitializerDeclarationNode initializer) {
                    InitializerDeclaration declaration = (InitializerDeclaration) context.declarations.get(initializer);
                    if (declaration == null) {
                        throw new TranslationRefusedException("Nested initializers require their own navigation ownership");
                    }
                    bodies.add(bodyProjection.read(initializer.body(), declaration.symbolId(),
                            enclosingType(context, initializer), List.of(), initializer.body().span()));
                    return TraversalAction.SKIP_CHILDREN;
                }
                if (node instanceof JavaVariableDeclaratorNode variable
                        && context.declarations.get(variable) instanceof FieldDeclaration declaration
                        && variable.initializer().isPresent()) {
                    bodies.add(bodyProjection.read(variable.initializer().get(), declaration.symbolId(),
                            enclosingType(context, variable), List.of(), declaration.variableSpan()));
                    return TraversalAction.SKIP_CHILDREN;
                }
                return TraversalAction.CONTINUE;
            }
        });
        return new JavaSourceFile(file.sourceName(), file.packageDeclaration(), file.moduleDeclaration(), file.imports(),
                file.types(), file.comments(), file.javaDocs(), file.problems(), file.typeReferences(), bodies);
    }

    private static SymbolId enclosingType(JavaProjectionContext context, Node node) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof JavaTypeDeclarationNode type) {
                return ((TypeDeclaration) context.declarations.get(type)).symbolId();
            }
        }
        throw new TranslationRefusedException("An executable declaration requires an enclosing type");
    }

}
