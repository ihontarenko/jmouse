package org.jmouse.java.parser.node.unit;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.declaration.JavaDeclarationNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.module.JavaModuleDeclarationNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed compilation-unit syntax, with role-owned children in source order. */
public final class JavaCompilationUnitNode extends JavaSyntaxNode {
    public static final NodeKind<JavaCompilationUnitNode> KIND = new NodeKind<>(JavaNodeKindNames.COMPILATION_UNIT, JavaCompilationUnitNode.class);
    public static final ChildRole<JavaPackageDeclarationNode> PACKAGE_DECLARATION = ChildRole.optional(JavaChildRoleNames.PACKAGE_DECLARATION, JavaPackageDeclarationNode.class);
    public static final ChildRole<JavaImportNode> IMPORTS = ChildRole.list(JavaChildRoleNames.IMPORTS, JavaImportNode.class);
    public static final ChildRole<JavaDeclarationNode> DECLARATIONS = ChildRole.list(JavaChildRoleNames.DECLARATIONS, JavaDeclarationNode.class);
    public static final ChildRole<JavaModuleDeclarationNode> MODULE = ChildRole.optional(JavaChildRoleNames.MODULE, JavaModuleDeclarationNode.class);
    public static final NodeSchema<JavaCompilationUnitNode> SCHEMA = NodeSchema.of(KIND, PACKAGE_DECLARATION, IMPORTS, DECLARATIONS, MODULE);

    public JavaCompilationUnitNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaPackageDeclarationNode> packageDeclaration() {
        return optionalChild(PACKAGE_DECLARATION);
    }

    public void setPackageDeclaration(JavaPackageDeclarationNode node) {
        setChild(PACKAGE_DECLARATION, node);
    }

    public List<JavaImportNode> imports() {
        return children(IMPORTS);
    }

    public void addImport(JavaImportNode node) {
        addChild(IMPORTS, node);
    }

    public List<JavaDeclarationNode> declarations() {
        return children(DECLARATIONS);
    }

    public void addDeclaration(JavaDeclarationNode node) {
        addChild(DECLARATIONS, node);
    }

    public Optional<JavaModuleDeclarationNode> module() {
        return optionalChild(MODULE);
    }

    public void setModule(JavaModuleDeclarationNode node) {
        setChild(MODULE, node);
    }
}

