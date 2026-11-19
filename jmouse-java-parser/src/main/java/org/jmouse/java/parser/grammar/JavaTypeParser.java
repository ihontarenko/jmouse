package org.jmouse.java.parser.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.node.type.JavaArrayTypeNode;
import org.jmouse.java.parser.node.type.JavaDiamondTypeArgumentsNode;
import org.jmouse.java.parser.node.type.JavaInferredTypeNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeSegmentNode;
import org.jmouse.java.parser.node.type.JavaPrimitiveKind;
import org.jmouse.java.parser.node.type.JavaPrimitiveTypeNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.node.type.JavaVoidTypeNode;

/** Source type syntax: generic segments, annotated dimensions and wildcard bounds. */
public final class JavaTypeParser implements ParseRule<JavaParsingInput, JavaTypeSyntaxNode> {
    @Override
    public JavaTypeSyntaxNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaTypeSyntaxNode element = element(input, session);
        List<JavaArrayDimensionNode> dimensions = dimensions(input, session);
        if (dimensions.isEmpty()) {
            return element;
        }
        if (element instanceof JavaVoidTypeNode || element instanceof JavaInferredTypeNode) {
            throw input.error("void or an inferred type cannot be an array element type");
        }
        JavaArrayTypeNode array = new JavaArrayTypeNode(input.name(), input.point());
        array.setElementType(element);
        dimensions.forEach(array::addDimension);
        return input.finish(array, start);
    }

    public static JavaTypeSyntaxNode element(JavaParsingInput input, ParseSession session) {
        return element(input, session, JavaTypeArgumentsForm.EXPLICIT);
    }

    public static JavaTypeSyntaxNode element(JavaParsingInput input, ParseSession session, JavaTypeArgumentsForm form) {
        int start = input.offset();
        List<JavaAnnotationNode> annotations = annotations(input, session);
        JavaPrimitiveKind primitive = primitive(input.token().value());
        if (primitive != null) {
            input.next(session);
            JavaPrimitiveTypeNode node = new JavaPrimitiveTypeNode(input.name(), input.point());
            node.setKind(primitive);
            annotations.forEach(node::addAnnotation);
            return input.finish(node, start);
        }
        if (input.take(JavaKeyword.VOID, session)) {
            if (!annotations.isEmpty()) {
                throw input.error("void does not accept type annotations");
            }
            return input.finish(new JavaVoidTypeNode(input.name(), input.point()), start);
        }
        if (input.take(JavaKeyword.VAR, session)) {
            input.requireFeature(JavaSourceFeatures.LOCAL_VARIABLE_INFERENCE);
            if (!annotations.isEmpty()) {
                throw input.error("An inferred type cannot carry type-use annotations");
            }
            return input.finish(new JavaInferredTypeNode(input.name(), input.point()), start);
        }
        JavaNamedTypeNode node = new JavaNamedTypeNode(input.name(), input.point());
        do {
            int segmentStart = annotations.isEmpty() ? input.offset() : annotations.getFirst().span().start().offset();
            JavaNamedTypeSegmentNode segment = new JavaNamedTypeSegmentNode(input.name(), input.point());
            annotations.forEach(segment::addAnnotation);
            segment.setName(input.identifier(session));
            int argumentsStart = input.offset();
            if (input.take("<", session)) {
                if (input.take(">", session)) {
                    if (form != JavaTypeArgumentsForm.DIAMOND_ALLOWED) {
                        throw input.errorAt("Diamond type arguments require class creation", input.spanFrom(argumentsStart));
                    }
                    segment.setDiamond(new JavaDiamondTypeArgumentsNode(input.name(), input.spanFrom(argumentsStart)));
                } else {
                    do {
                        segment.addArgument(session.parse(JavaParseRoles.TYPE_ARGUMENT, input));
                    } while (input.take(",", session));
                    input.expect(">", session);
                }
            }
            node.addSegment(input.finish(segment, segmentStart));
            if (segment.diamond().isPresent() && input.matches(".")) {
                throw input.error("Diamond must occur on the instantiated type's final segment");
            }
            if (!input.matches(".") || !(JavaToken.IDENTIFIER.matches(input.cursor().lookAt(1))
                    || input.cursor().lookAt(1).value().equals("@"))) {
                break;
            }
            input.expect(".", session);
            annotations = annotations(input, session);
        } while (true);
        input.requireTypeIdentifier(node.segments().getLast().name());
        return input.finish(node, start);
    }

    public static void explicitArguments(JavaParsingInput input, ParseSession session,
                                          Consumer<JavaTypeSyntaxNode> destination) {
        if (input.take("<", session)) {
            do {
                destination.accept(session.parse(JavaParseRoles.REFERENCE_TYPE, input));
            } while (input.take(",", session));
            input.expect(">", session);
        }
    }

    public static List<JavaArrayDimensionNode> dimensions(JavaParsingInput input, ParseSession session) {
        List<JavaArrayDimensionNode> dimensions = new ArrayList<>();
        while (input.matches("[") || input.matches("@")) {
            int start = input.offset();
            JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
            List<JavaAnnotationNode> annotations = annotations(input, session);
            if (!input.take("[", session)) {
                input.restore(checkpoint);
                break;
            }
            input.expect("]", session);
            JavaArrayDimensionNode dimension = new JavaArrayDimensionNode(input.name(), input.point());
            annotations.forEach(dimension::addAnnotation);
            dimensions.add(input.finish(dimension, start));
        }
        return dimensions;
    }

    public static List<JavaAnnotationNode> annotations(JavaParsingInput input, ParseSession session) {
        List<JavaAnnotationNode> annotations = new ArrayList<>();
        while (input.matches("@") && !input.cursor().lookAt(1).value().equals(JavaKeyword.INTERFACE.spelling())) {
            annotations.add(session.parse(JavaParseRoles.ANNOTATION, input));
        }
        return annotations;
    }

    public static JavaPrimitiveKind primitive(String spelling) {
        return JavaKeyword.fromSpelling(spelling).map(keyword -> switch (keyword) {
            case BOOLEAN -> JavaPrimitiveKind.BOOLEAN;
            case BYTE -> JavaPrimitiveKind.BYTE;
            case SHORT -> JavaPrimitiveKind.SHORT;
            case CHAR -> JavaPrimitiveKind.CHAR;
            case INT -> JavaPrimitiveKind.INT;
            case LONG -> JavaPrimitiveKind.LONG;
            case FLOAT -> JavaPrimitiveKind.FLOAT;
            case DOUBLE -> JavaPrimitiveKind.DOUBLE;
            default -> null;
        }).orElse(null);
    }
}
