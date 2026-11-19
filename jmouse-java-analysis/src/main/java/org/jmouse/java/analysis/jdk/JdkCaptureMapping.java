package org.jmouse.java.analysis.jdk;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import org.jmouse.java.analysis.JavaCapturedExpression;
import org.jmouse.java.analysis.JavaCapturedSymbol;
import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.symbol.JavaTypeKey;
import org.jmouse.java.model.symbol.JavaVariableKey;
import org.jmouse.java.model.symbol.JavaVariableKind;
import org.jmouse.java.model.type.JavaType;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodReferenceExpressionNode;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;

final class JdkCaptureMapping {
    private final Trees trees;
    private final Types types;
    private final JdkSourceIndex source;
    private final JdkSymbolMapping symbols;

    JdkCaptureMapping(Trees trees, Types types, JdkSourceIndex source, JdkSymbolMapping symbols) {
        this.trees = trees;
        this.types = types;
        this.source = source;
        this.symbols = symbols;
    }

    void reference(TreePath path, Element element) {
        if (!(path.getLeaf() instanceof IdentifierTree
                        || path.getLeaf() instanceof MemberSelectTree)
                || element == null) {
            return;
        }
        if (element instanceof VariableElement variable && variable.getConstantValue() == null) {
            if (variable.getKind() != ElementKind.FIELD
                    && variable.getKind() != ElementKind.ENUM_CONSTANT) {
                TreePath declaration = trees.getPath(variable);
                if (declaration != null) {
                    capture(
                            path,
                            symbols.key(variable),
                            symbols.typeMapping.type(variable.asType()),
                            trees.getSourcePositions()
                                    .getStartPosition(
                                            declaration.getCompilationUnit(),
                                            declaration.getLeaf()),
                            trees.getSourcePositions()
                                    .getEndPosition(
                                            declaration.getCompilationUnit(),
                                            declaration.getLeaf()),
                            null);
                }
                return;
            }
            String name = variable.getSimpleName().toString();
            if (name.equals(JavaKeyword.THIS.spelling())
                    || name.equals(JavaKeyword.SUPER.spelling())) {
                TypeElement owner = (TypeElement) variable.getEnclosingElement();
                receiver(path, owner);
                return;
            }
        }
        if (path.getLeaf() instanceof IdentifierTree
                && !element.getModifiers().contains(Modifier.STATIC)
                && (element.getKind() == ElementKind.METHOD
                        || element.getKind() == ElementKind.FIELD)
                && !(element instanceof VariableElement variable
                        && variable.getConstantValue() != null)) {
            TypeElement owner = (TypeElement) element.getEnclosingElement();
            for (TreePath parent = path.getParentPath();
                    parent != null;
                    parent = parent.getParentPath()) {
                if (parent.getLeaf() instanceof ClassTree
                        && trees.getElement(parent) instanceof TypeElement candidate
                        && types.isSubtype(
                                types.erasure(candidate.asType()), types.erasure(owner.asType()))) {
                    receiver(path, candidate);
                    return;
                }
            }
        }
    }

    private void receiver(TreePath path, TypeElement owner) {
        JavaTypeKey type = symbols.typeKey(owner);
        capture(
                path,
                new JavaVariableKey(
                        type, JavaKeyword.THIS.spelling(), 0, JavaVariableKind.RECEIVER),
                symbols.typeMapping.type(owner.asType()),
                -1,
                -1,
                type);
    }

    private void capture(
            TreePath reference,
            JavaSymbolKey symbol,
            JavaType type,
            long declarationStart,
            long declarationEnd,
            JavaTypeKey receiver) {
        for (TreePath parent = reference.getParentPath();
                parent != null;
                parent = parent.getParentPath()) {
            Tree closure = parent.getLeaf();
            if (closure instanceof ClassTree
                    && trees.getElement(parent) instanceof TypeElement owner) {
                if (receiver != null && symbols.typeKey(owner).equals(receiver)) {
                    return;
                }
                if (owner.getNestingKind() != NestingKind.LOCAL
                        && owner.getNestingKind() != NestingKind.ANONYMOUS) {
                    continue;
                }
            } else if (!(closure instanceof LambdaExpressionTree)) {
                continue;
            }
            long start =
                    trees.getSourcePositions()
                            .getStartPosition(parent.getCompilationUnit(), closure);
            long end =
                    trees.getSourcePositions().getEndPosition(parent.getCompilationUnit(), closure);
            if (declarationStart >= start
                    && declarationEnd >= declarationStart
                    && declarationEnd <= end) {
                continue;
            }
            for (JavaSyntaxNode node : source.nodes(parent.getCompilationUnit(), start, end)) {
                if (node instanceof JavaLambdaExpressionNode
                        || node instanceof JavaTypeDeclarationNode
                        || node instanceof JavaAnonymousClassBodyNode) {
                    source.capture(node, new JavaCapturedSymbol(symbol, type));
                }
            }
        }
    }

    void methodReference(TreePath path, MemberReferenceTree reference) {
        if (reference.getMode() == MemberReferenceTree.ReferenceMode.NEW) {
            return;
        }
        TreePath qualifier = new TreePath(path, reference.getQualifierExpression());
        Element selected = trees.getElement(qualifier);
        if (selected instanceof TypeElement
                || selected != null && selected.getKind() == ElementKind.TYPE_PARAMETER) {
            return;
        }
        long start =
                trees.getSourcePositions().getStartPosition(path.getCompilationUnit(), reference);
        long end = trees.getSourcePositions().getEndPosition(path.getCompilationUnit(), reference);
        for (JavaSyntaxNode node : source.nodes(path.getCompilationUnit(), start, end)) {
            if (node instanceof JavaMethodReferenceExpressionNode methodReference) {
                source.capture(
                        node,
                        new JavaCapturedExpression(
                                source.identities.get(methodReference.qualifier()),
                                symbols.typeMapping.type(trees.getTypeMirror(qualifier))));
            }
        }
    }
}
