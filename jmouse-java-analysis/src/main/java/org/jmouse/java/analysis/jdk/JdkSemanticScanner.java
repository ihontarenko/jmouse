package org.jmouse.java.analysis.jdk;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import org.jmouse.java.analysis.JavaConversion;
import org.jmouse.java.analysis.JavaDispatchKind;
import org.jmouse.java.analysis.JavaFunctionalTarget;
import org.jmouse.java.analysis.JavaIntrinsicOperation;
import org.jmouse.java.analysis.JavaInvocation;
import org.jmouse.java.analysis.JavaInvocationArity;
import org.jmouse.java.analysis.JavaScopeView;
import org.jmouse.java.model.symbol.JavaMethodKey;
import org.jmouse.java.model.symbol.JavaSymbolKey;
import org.jmouse.java.model.type.JavaExecutableType;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.expression.JavaArrayCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaClassInstanceCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaLiteralNode;
import org.jmouse.java.parser.node.expression.JavaMethodInvocationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodReferenceExpressionNode;
import org.jmouse.java.parser.node.expression.JavaSwitchExpressionNode;
import org.jmouse.java.parser.node.statement.JavaBreakStatementNode;
import org.jmouse.java.parser.node.statement.JavaContinueStatementNode;
import org.jmouse.java.parser.node.statement.JavaStatementNode;
import org.jmouse.java.parser.node.statement.JavaYieldStatementNode;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

final class JdkSemanticScanner extends TreePathScanner<Void, Void> {
    private enum Transfer {
        BREAK,
        CONTINUE,
        YIELD
    }

    private final CompilationUnitTree unit;
    private final Trees trees;
    private final Types types;
    private final JdkSourceIndex source;
    private final JdkSymbolMapping symbols;
    private final JdkConversions conversions;
    private final JdkScopeMapping scopes;
    private final JdkCaptureMapping captures;
    private final JdkOverrideMapping overrides;
    private final JdkFunctionalMapping functions;
    private final JdkIntrinsicMapping intrinsics;

    JdkSemanticScanner(
            CompilationUnitTree unit,
            Trees trees,
            Types types,
            Elements elements,
            JdkSourceIndex source,
            JdkSymbolMapping symbols) {
        this.unit = unit;
        this.trees = trees;
        this.types = types;
        this.source = source;
        this.symbols = symbols;
        conversions = new JdkConversions(types, symbols.typeMapping);
        scopes = new JdkScopeMapping(trees, symbols);
        captures = new JdkCaptureMapping(trees, types, source, symbols);
        overrides = new JdkOverrideMapping(elements, types, symbols);
        functions = new JdkFunctionalMapping(elements, types, symbols);
        intrinsics = new JdkIntrinsicMapping(trees, elements);
    }

    @Override
    public Void scan(Tree tree, Void unused) {
        if (tree == null) {
            return null;
        }
        TreePath path =
                tree instanceof CompilationUnitTree compilationUnit
                        ? new TreePath(compilationUnit)
                        : new TreePath(getCurrentPath(), tree);
        bind(path);
        return super.scan(tree, unused);
    }

    private void bind(TreePath path) {
        Tree tree = path.getLeaf();
        Element element = trees.getElement(path);
        JavaIntrinsicOperation intrinsic = intrinsics.operation(path);
        JavaSymbolKey symbol = symbols.describe(intrinsics.declaration(intrinsic, element));
        captures.reference(path, element);
        TypeMirror type = trees.getTypeMirror(path);
        if (type == null && element != null) {
            type = element.asType();
        }
        long start = trees.getSourcePositions().getStartPosition(unit, tree);
        long end = trees.getSourcePositions().getEndPosition(unit, tree);
        List<JavaSyntaxNode> nodes = source.nodes(unit, start, end);
        JavaScopeView scope =
                nodes.stream()
                                .anyMatch(
                                        node ->
                                                node instanceof JavaExpressionNode
                                                        && !(node instanceof JavaLiteralNode))
                        ? scopes.scope(path)
                        : null;
        for (JavaSyntaxNode node : nodes) {
            source.bind(node, symbols.typeMapping.type(type), symbol);
            if (intrinsic != null) {
                source.intrinsics.put(source.identities.get(node), intrinsic);
            }
            if (node instanceof JavaLambdaExpressionNode
                    || node instanceof JavaMethodReferenceExpressionNode) {
                JavaFunctionalTarget target = functions.target(type);
                if (target != null) {
                    source.functionalTargets.put(source.identities.get(node), target);
                }
            }
            if (scope != null) {
                source.scopes.put(source.identities.get(node), scope);
            }
        }
    }

    @Override
    public Void visitClass(ClassTree tree, Void unused) {
        super.visitClass(tree, unused);
        if (trees.getElement(getCurrentPath()) instanceof TypeElement owner) {
            overrides.collect(owner);
        }
        return null;
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
        super.visitVariable(tree, unused);
        Element element = trees.getElement(getCurrentPath());
        if (element != null && tree.getInitializer() != null) {
            conversion(tree.getInitializer(), element.asType(), false);
        }
        JdkSourceIndex.VariableAnchor anchor =
                source.variable(unit, tree, trees.getSourcePositions());
        if (anchor != null && element != null) {
            JavaSymbolKey symbol = symbols.describe(element);
            if (anchor.declaration() instanceof JavaRecordComponentNode
                    && element.getEnclosingElement() instanceof TypeElement owner) {
                symbol =
                        owner.getRecordComponents().stream()
                                .filter(
                                        component ->
                                                component
                                                        .getSimpleName()
                                                        .contentEquals(tree.getName()))
                                .map(symbols::describe)
                                .findFirst()
                                .orElse(symbol);
            }
            source.bind(anchor.declaration(), symbols.typeMapping.type(element.asType()), symbol);
            source.bind(anchor.name(), symbols.typeMapping.type(element.asType()), symbol);
        }
        return null;
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, Void unused) {
        super.visitNewArray(tree, unused);
        TypeMirror type = trees.getTypeMirror(getCurrentPath());
        for (JavaSyntaxNode node : nodes(tree)) {
            if (node instanceof JavaArrayCreationExpressionNode creation) {
                creation.initializer()
                        .ifPresent(
                                initializer ->
                                        source.bind(
                                                initializer, symbols.typeMapping.type(type), null));
            }
        }
        if (type instanceof ArrayType array && tree.getInitializers() != null) {
            for (ExpressionTree initializer : tree.getInitializers()) {
                conversion(initializer, array.getComponentType(), false);
            }
        }
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void unused) {
        super.visitAssignment(tree, unused);
        conversion(
                tree.getExpression(),
                trees.getTypeMirror(new TreePath(getCurrentPath(), tree.getVariable())),
                false);
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, Void unused) {
        super.visitTypeCast(tree, unused);
        conversion(tree.getExpression(), trees.getTypeMirror(getCurrentPath()), true);
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree tree, Void unused) {
        super.visitReturn(tree, unused);
        if (tree.getExpression() != null) {
            for (TreePath parent = getCurrentPath().getParentPath();
                    parent != null;
                    parent = parent.getParentPath()) {
                if (parent.getLeaf() instanceof LambdaExpressionTree) {
                    break;
                }
                if (parent.getLeaf() instanceof MethodTree
                        && trees.getElement(parent) instanceof ExecutableElement method) {
                    conversion(tree.getExpression(), method.getReturnType(), false);
                    break;
                }
            }
        }
        return null;
    }

    private void conversion(ExpressionTree expression, TypeMirror target, boolean explicit) {
        TypeMirror actual = trees.getTypeMirror(new TreePath(getCurrentPath(), expression));
        if (actual == null
                || target == null
                || actual.getKind() == TypeKind.ERROR
                || target.getKind() == TypeKind.ERROR) {
            return;
        }
        JavaConversion conversion = conversions.conversion(actual, target, expression, explicit);
        for (JavaSyntaxNode node : nodes(expression)) {
            if (node instanceof JavaExpressionNode) {
                source.conversion(node, conversion);
            }
        }
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        super.visitMethodInvocation(tree, unused);
        TreePath path = getCurrentPath();
        if (trees.getElement(path) instanceof ExecutableElement executable) {
            TypeMirror selected = trees.getTypeMirror(new TreePath(path, tree.getMethodSelect()));
            ExecutableType signature =
                    selected instanceof ExecutableType method
                            ? method
                            : (ExecutableType) executable.asType();
            JavaInvocation invocation =
                    invocation(
                            path,
                            executable,
                            signature,
                            tree.getArguments(),
                            superSelection(tree.getMethodSelect()));
            for (JavaSyntaxNode node : nodes(tree)) {
                if (node instanceof JavaMethodInvocationExpressionNode) {
                    source.invocation(node, invocation);
                }
            }
            if (executable.getKind() == ElementKind.CONSTRUCTOR
                    && path.getParentPath() != null
                    && path.getParentPath().getLeaf()
                            instanceof ExpressionStatementTree statement) {
                for (JavaSyntaxNode node : nodes(statement)) {
                    if (node instanceof JavaConstructorInvocationNode) {
                        source.invocation(node, invocation);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void unused) {
        super.visitNewClass(tree, unused);
        TreePath path = getCurrentPath();
        if (trees.getElement(path) instanceof ExecutableElement executable) {
            TypeMirror receiver = trees.getTypeMirror(path);
            ExecutableType signature =
                    receiver instanceof DeclaredType declared
                            ? (ExecutableType) types.asMemberOf(declared, executable)
                            : (ExecutableType) executable.asType();
            JavaInvocation invocation =
                    invocation(path, executable, signature, tree.getArguments(), false);
            for (JavaSyntaxNode node : nodes(tree)) {
                if (node instanceof JavaClassInstanceCreationExpressionNode) {
                    source.invocation(node, invocation);
                }
            }
        }
        return null;
    }

    private JavaInvocation invocation(
            TreePath path,
            ExecutableElement executable,
            ExecutableType signature,
            List<? extends ExpressionTree> arguments,
            boolean superSelection) {
        List<TypeMirror> actual =
                arguments.stream()
                        .map(argument -> trees.getTypeMirror(new TreePath(path, argument)))
                        .toList();
        List<? extends TypeMirror> formal = signature.getParameterTypes();
        JavaInvocationArity arity = JavaInvocationArity.FIXED;
        if (executable.isVarArgs()
                && (actual.size() != formal.size()
                        || actual.isEmpty()
                        || !types.isAssignable(actual.getLast(), formal.getLast()))) {
            arity = JavaInvocationArity.VARIABLE;
        }
        List<JavaConversion> argumentConversions = new ArrayList<>();
        for (int index = 0; index < actual.size(); index++) {
            TypeMirror target = index < formal.size() ? formal.get(index) : formal.getLast();
            if (arity == JavaInvocationArity.VARIABLE && index >= formal.size() - 1) {
                target = ((ArrayType) formal.getLast()).getComponentType();
            }
            argumentConversions.add(
                    conversions.conversion(actual.get(index), target, arguments.get(index), false));
        }
        JavaDispatchKind dispatch =
                executable.getKind() == ElementKind.CONSTRUCTOR
                                || superSelection
                                || executable.getModifiers().contains(Modifier.PRIVATE)
                        ? JavaDispatchKind.SPECIAL
                        : executable.getModifiers().contains(Modifier.STATIC)
                                ? JavaDispatchKind.STATIC
                                : executable.getEnclosingElement().getKind().isInterface()
                                        ? JavaDispatchKind.INTERFACE
                                        : JavaDispatchKind.VIRTUAL;
        return new JavaInvocation(
                (JavaMethodKey)
                        symbols.describe(
                                intrinsics.declaration(intrinsics.operation(path), executable)),
                dispatch,
                arity,
                (JavaExecutableType) symbols.typeMapping.type(signature),
                actual.stream().map(symbols.typeMapping::type).toList(),
                symbols.typeMapping.type(trees.getTypeMirror(path)),
                argumentConversions);
    }

    private List<JavaSyntaxNode> nodes(Tree tree) {
        return source.nodes(
                unit,
                trees.getSourcePositions().getStartPosition(unit, tree),
                trees.getSourcePositions().getEndPosition(unit, tree));
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
        super.visitMemberReference(tree, unused);
        captures.methodReference(getCurrentPath(), tree);
        return null;
    }

    @Override
    public Void visitBreak(BreakTree tree, Void unused) {
        control(
                tree,
                target(
                        tree.getLabel() == null ? null : tree.getLabel().toString(),
                        Transfer.BREAK));
        return super.visitBreak(tree, unused);
    }

    @Override
    public Void visitContinue(ContinueTree tree, Void unused) {
        control(
                tree,
                target(
                        tree.getLabel() == null ? null : tree.getLabel().toString(),
                        Transfer.CONTINUE));
        return super.visitContinue(tree, unused);
    }

    @Override
    public Void visitYield(YieldTree tree, Void unused) {
        control(tree, target(null, Transfer.YIELD));
        return super.visitYield(tree, unused);
    }

    private Tree target(String label, Transfer transfer) {
        for (TreePath parent = getCurrentPath().getParentPath();
                parent != null;
                parent = parent.getParentPath()) {
            Tree tree = parent.getLeaf();
            if (tree.getKind() == Tree.Kind.METHOD
                    || tree.getKind() == Tree.Kind.LAMBDA_EXPRESSION
                    || tree instanceof ClassTree) {
                return null;
            }
            if (label != null) {
                if (tree instanceof LabeledStatementTree labelled
                        && labelled.getLabel().contentEquals(label)) {
                    return transfer == Transfer.CONTINUE ? labelled.getStatement() : labelled;
                }
                continue;
            }
            if (transfer == Transfer.YIELD) {
                if (tree.getKind() == Tree.Kind.SWITCH_EXPRESSION) {
                    return tree;
                }
                continue;
            }
            if (tree.getKind() == Tree.Kind.WHILE_LOOP
                    || tree.getKind() == Tree.Kind.DO_WHILE_LOOP
                    || tree.getKind() == Tree.Kind.FOR_LOOP
                    || tree.getKind() == Tree.Kind.ENHANCED_FOR_LOOP
                    || transfer == Transfer.BREAK && tree.getKind() == Tree.Kind.SWITCH) {
                return tree;
            }
        }
        return null;
    }

    private void control(Tree transfer, Tree target) {
        if (target == null) {
            return;
        }
        JavaSyntaxNode destination =
                nodes(target).stream()
                        .filter(
                                node ->
                                        node instanceof JavaStatementNode
                                                || node instanceof JavaSwitchExpressionNode)
                        .findFirst()
                        .orElse(null);
        if (destination != null) {
            for (JavaSyntaxNode node : nodes(transfer)) {
                if (node instanceof JavaBreakStatementNode
                        || node instanceof JavaContinueStatementNode
                        || node instanceof JavaYieldStatementNode) {
                    source.control(node, destination);
                }
            }
        }
    }

    private static boolean superSelection(Tree tree) {
        if (tree instanceof IdentifierTree identifier) {
            return identifier.getName().contentEquals(JavaKeyword.SUPER.spelling());
        }
        return tree instanceof MemberSelectTree select
                && (select.getIdentifier().contentEquals(JavaKeyword.SUPER.spelling())
                        || superSelection(select.getExpression()));
    }
}
