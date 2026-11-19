package org.jmouse.java.analysis.jdk;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import org.jmouse.java.analysis.JavaIntrinsicOperation;
import org.jmouse.java.analysis.JavaStandardIntrinsicOperation;
import org.jmouse.java.model.symbol.JavaPlatformMemberNames;
import org.jmouse.java.model.type.JavaPlatformTypeNames;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.ArrayType;
import javax.lang.model.util.Elements;

final class JdkIntrinsicMapping {
    private final Trees trees;
    private final Elements elements;

    JdkIntrinsicMapping(Trees trees, Elements elements) {
        this.trees = trees;
        this.elements = elements;
    }

    JavaIntrinsicOperation operation(TreePath path) {
        Tree tree = path.getLeaf();
        if (tree instanceof MethodInvocationTree invocation) {
            return operation(new TreePath(path, invocation.getMethodSelect()));
        }
        Tree qualifier;
        String name;
        if (tree instanceof MemberSelectTree select) {
            qualifier = select.getExpression();
            name = select.getIdentifier().toString();
        } else if (tree instanceof MemberReferenceTree reference) {
            qualifier = reference.getQualifierExpression();
            name = reference.getName().toString();
        } else {
            return null;
        }
        if (!(trees.getTypeMirror(new TreePath(path, qualifier)) instanceof ArrayType)) {
            return null;
        }
        if (tree instanceof MemberReferenceTree reference
                && reference.getMode() == MemberReferenceTree.ReferenceMode.NEW) {
            return JavaStandardIntrinsicOperation.ARRAY_CONSTRUCTOR_REFERENCE;
        }
        if (name.equals(JavaPlatformMemberNames.ARRAY_LENGTH)) {
            return JavaStandardIntrinsicOperation.ARRAY_LENGTH;
        }
        if (name.equals(JavaPlatformMemberNames.CLONE)) {
            return JavaStandardIntrinsicOperation.ARRAY_CLONE;
        }
        return null;
    }

    Element declaration(JavaIntrinsicOperation intrinsic, Element selected) {
        if (intrinsic == JavaStandardIntrinsicOperation.ARRAY_LENGTH
                || intrinsic == JavaStandardIntrinsicOperation.ARRAY_CONSTRUCTOR_REFERENCE) {
            return null;
        }
        if (intrinsic == JavaStandardIntrinsicOperation.ARRAY_CLONE) {
            return elements
                    .getTypeElement(JavaPlatformTypeNames.OBJECT)
                    .getEnclosedElements()
                    .stream()
                    .filter(
                            member ->
                                    member.getKind() == ElementKind.METHOD
                                            && member.getSimpleName()
                                                    .contentEquals(JavaPlatformMemberNames.CLONE))
                    .findFirst()
                    .orElseThrow();
        }
        return selected;
    }
}
