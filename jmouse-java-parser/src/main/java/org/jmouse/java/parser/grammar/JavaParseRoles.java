package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRole;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.node.expression.JavaArrayInitializerNode;
import org.jmouse.java.parser.node.expression.JavaCastExpressionNode;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaLiteralNode;
import org.jmouse.java.parser.node.expression.JavaSwitchExpressionNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaReceiverParameterNode;
import org.jmouse.java.parser.node.pattern.JavaPatternNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

/** Stable, replaceable production identities; concrete rule classes are configuration. */
public final class JavaParseRoles {
    public static final ParseRole<JavaParsingInput, JavaSyntaxNode> ANNOTATION_VALUE =
            new ParseRole<>(JavaParseRoleNames.ANNOTATION_VALUE, JavaParsingInput.class, JavaSyntaxNode.class);
    public static final ParseRole<JavaParsingInput, JavaTypeSyntaxNode> TYPE_ARGUMENT =
            new ParseRole<>(JavaParseRoleNames.TYPE_ARGUMENT, JavaParsingInput.class, JavaTypeSyntaxNode.class);
    public static final ParseRole<JavaParsingInput, JavaTypeSyntaxNode> REFERENCE_TYPE =
            new ParseRole<>(JavaParseRoleNames.REFERENCE_TYPE, JavaParsingInput.class, JavaTypeSyntaxNode.class);
    public static final ParseRole<JavaParsingInput, JavaModifiersNode> VARIABLE_MODIFIERS =
            new ParseRole<>(JavaParseRoleNames.VARIABLE_MODIFIERS, JavaParsingInput.class, JavaModifiersNode.class);
    public static final ParseRole<JavaParsingInput, JavaExpressionNode> CONDITIONAL_EXPRESSION =
            new ParseRole<>(JavaParseRoleNames.CONDITIONAL_EXPRESSION, JavaParsingInput.class, JavaExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaConstructorInvocationNode> CONSTRUCTOR_INVOCATION =
            new ParseRole<>(JavaParseRoleNames.CONSTRUCTOR_INVOCATION, JavaParsingInput.class, JavaConstructorInvocationNode.class);
    public static final ParseRole<JavaParsingInput, JavaReceiverParameterNode> RECEIVER_PARAMETER =
            new ParseRole<>(JavaParseRoleNames.RECEIVER_PARAMETER, JavaParsingInput.class, JavaReceiverParameterNode.class);
    public static final ParseRole<JavaParsingInput, JavaSwitchExpressionNode> SWITCH_EXPRESSION =
            new ParseRole<>(JavaParseRoleNames.SWITCH_EXPRESSION, JavaParsingInput.class, JavaSwitchExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaAnonymousClassBodyNode> ANONYMOUS_CLASS_BODY =
            new ParseRole<>(JavaParseRoleNames.ANONYMOUS_CLASS_BODY, JavaParsingInput.class, JavaAnonymousClassBodyNode.class);
    public static final ParseRole<JavaParsingInput, JavaParameterNode> PARAMETER =
            new ParseRole<>(JavaParseRoleNames.PARAMETER, JavaParsingInput.class, JavaParameterNode.class);
    public static final ParseRole<JavaParsingInput, JavaExpressionNode> PRIMARY =
            new ParseRole<>(JavaParseRoleNames.PRIMARY_EXPRESSION, JavaParsingInput.class, JavaExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaExpressionNode> CREATION =
            new ParseRole<>(JavaParseRoleNames.CREATION_EXPRESSION, JavaParsingInput.class, JavaExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaLambdaExpressionNode> LAMBDA =
            new ParseRole<>(JavaParseRoleNames.LAMBDA, JavaParsingInput.class, JavaLambdaExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaArrayInitializerNode> ARRAY_INITIALIZER =
            new ParseRole<>(JavaParseRoleNames.ARRAY_INITIALIZER, JavaParsingInput.class, JavaArrayInitializerNode.class);
    public static final ParseRole<JavaParsingInput, JavaExpressionNode> VARIABLE_INITIALIZER =
            new ParseRole<>(JavaParseRoleNames.VARIABLE_INITIALIZER, JavaParsingInput.class, JavaExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaModifiersNode> MODIFIERS =
            new ParseRole<>(JavaParseRoleNames.MODIFIERS, JavaParsingInput.class, JavaModifiersNode.class);
    public static final ParseRole<JavaParsingInput, JavaPatternNode> PATTERN =
            new ParseRole<>(JavaParseRoleNames.PATTERN, JavaParsingInput.class, JavaPatternNode.class);
    public static final ParseRole<JavaParsingInput, JavaCastExpressionNode> CAST =
            new ParseRole<>(JavaParseRoleNames.CAST, JavaParsingInput.class, JavaCastExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaExpressionNode> UNARY_EXPRESSION =
            new ParseRole<>(JavaParseRoleNames.UNARY_EXPRESSION, JavaParsingInput.class, JavaExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaTypeSyntaxNode> TYPE =
            new ParseRole<>(JavaParseRoleNames.TYPE, JavaParsingInput.class, JavaTypeSyntaxNode.class);
    public static final ParseRole<JavaParsingInput, JavaAnnotationNode> ANNOTATION =
            new ParseRole<>(JavaParseRoleNames.ANNOTATION, JavaParsingInput.class, JavaAnnotationNode.class);
    public static final ParseRole<JavaParsingInput, JavaQualifiedNameNode> QUALIFIED_NAME =
            new ParseRole<>(JavaParseRoleNames.QUALIFIED_NAME, JavaParsingInput.class, JavaQualifiedNameNode.class);
    public static final ParseRole<JavaParsingInput, JavaExpressionNode> EXPRESSION =
            new ParseRole<>(JavaParseRoleNames.EXPRESSION, JavaParsingInput.class, JavaExpressionNode.class);
    public static final ParseRole<JavaParsingInput, JavaLiteralNode> LITERAL =
            new ParseRole<>(JavaParseRoleNames.LITERAL, JavaParsingInput.class, JavaLiteralNode.class);

    private JavaParseRoles() {
    }
}
