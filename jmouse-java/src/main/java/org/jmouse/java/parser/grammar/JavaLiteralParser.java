package org.jmouse.java.parser.grammar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.expression.JavaBooleanLiteralNode;
import org.jmouse.java.parser.node.expression.JavaCharacterLiteralNode;
import org.jmouse.java.parser.node.expression.JavaFloatingLiteralValue;
import org.jmouse.java.parser.node.expression.JavaFloatingPointLiteralNode;
import org.jmouse.java.parser.node.expression.JavaIntegerLiteralNode;
import org.jmouse.java.parser.node.expression.JavaIntegerLiteralKind;
import org.jmouse.java.parser.node.expression.JavaLiteralNode;
import org.jmouse.java.parser.node.expression.JavaNullLiteralNode;
import org.jmouse.java.parser.node.expression.JavaStringLiteralNode;
import org.jmouse.java.parser.node.expression.JavaTextBlockLiteralNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Literal grammar and Java values, independent of the jME expression evaluator. */
public final class JavaLiteralParser implements ParseRule<JavaParsingInput, JavaLiteralNode> {
    private enum NumericKind { INTEGER, FLOATING }
    private record NumericForm(NumericKind kind, int radix, Pattern pattern) {}
    private static final List<NumericForm> FORMS = loadForms();

    @Override
    public JavaLiteralNode parse(JavaParsingInput input, ParseSession session) {
        Token token = input.next(session);
        SourceSpan span = input.source().span(token);
        String raw = span.slice(input.source());
        String text = token.value();
        if (!(token.type() instanceof JavaToken kind)) {
            throw input.errorAt("Expected a Java literal", span);
        }
        return switch (kind) {
            case NUMBER_LITERAL -> number(input, raw, text, span);
            case BOOLEAN_LITERAL -> {
                JavaBooleanLiteralNode node = new JavaBooleanLiteralNode(input.name(), span);
                node.setRaw(raw);
                node.setValue(Boolean.parseBoolean(text));
                yield node;
            }
            case NULL_LITERAL -> new JavaNullLiteralNode(input.name(), span);
            case STRING_LITERAL -> {
                JavaStringLiteralNode node = new JavaStringLiteralNode(input.name(), span);
                node.setRaw(raw);
                node.setValue(decode(input, text, span, false));
                yield node;
            }
            case CHARACTER_LITERAL -> {
                String value = decode(input, text, span, false);
                if (value.length() != 1) {
                    throw input.errorAt("A character literal must contain exactly one UTF-16 code unit", span);
                }
                JavaCharacterLiteralNode node = new JavaCharacterLiteralNode(input.name(), span);
                node.setRaw(raw);
                node.setValue(value.charAt(0));
                yield node;
            }
            case TEXT_BLOCK -> {
                input.requireFeature(JavaSourceFeatures.TEXT_BLOCKS);
                JavaTextBlockLiteralNode node = new JavaTextBlockLiteralNode(input.name(), span);
                node.setRaw(raw);
                node.setValue(decode(input, text, span, true));
                yield node;
            }
            default -> throw input.errorAt("Expected a Java literal", span);
        };
    }

    private JavaLiteralNode number(JavaParsingInput input, String raw, String text,
                                   SourceSpan span) {
        NumericForm form = FORMS.stream().filter(candidate -> candidate.pattern().matcher(text).matches())
                .findFirst().orElseThrow(() -> input.errorAt("Malformed numeric literal", span));
        String normalized = text.replace("_", "");
        if (form.kind() == NumericKind.INTEGER) {
            JavaIntegerLiteralKind kind = JavaIntegerLiteralKind.INT;
            if (normalized.endsWith("l") || normalized.endsWith("L")) {
                kind = JavaIntegerLiteralKind.LONG;
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            if (form.radix() == 16 || form.radix() == 2) {
                normalized = normalized.substring(2);
            }
            JavaIntegerLiteralNode node = new JavaIntegerLiteralNode(input.name(), span);
            node.setRaw(raw);
            node.setKind(kind);
            node.setValue(new BigInteger(normalized, form.radix()));
            return node;
        }
        JavaFloatingPointLiteralNode node = new JavaFloatingPointLiteralNode(input.name(), span);
        node.setRaw(raw);
        if (normalized.endsWith("f") || normalized.endsWith("F")) {
            float value = Float.parseFloat(normalized);
            if (!Float.isFinite(value) || value == 0 && nonzeroSignificand(normalized)) {
                throw input.errorAt("Floating literal is outside the float range", span);
            }
            node.setValue(new JavaFloatingLiteralValue.Binary32(value));
        } else {
            double value = Double.parseDouble(normalized);
            if (!Double.isFinite(value) || value == 0 && nonzeroSignificand(normalized)) {
                throw input.errorAt("Floating literal is outside the double range", span);
            }
            node.setValue(new JavaFloatingLiteralValue.Binary64(value));
        }
        return node;
    }

    private static boolean nonzeroSignificand(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        boolean hexadecimal = lower.startsWith("0x");
        int end = lower.indexOf(hexadecimal ? 'p' : 'e');
        if (end < 0) {
            end = lower.length();
            char suffix = lower.charAt(end - 1);
            if (suffix == 'f' || suffix == 'd') {
                end--;
            }
        }
        for (int index = hexadecimal ? 2 : 0; index < end; index++) {
            if (Character.digit(lower.charAt(index), hexadecimal ? 16 : 10) > 0) {
                return true;
            }
        }
        return false;
    }

    private String decode(JavaParsingInput input, String text,
                          SourceSpan span, boolean textBlock) {
        try {
            if (!textBlock) {
                if (text.length() < 2 || text.charAt(text.length() - 1) != text.charAt(0)) {
                    throw input.errorAt("Unterminated literal", span);
                }
                return text.substring(1, text.length() - 1).translateEscapes();
            }
            String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
            int openingLine = normalized.indexOf('\n');
            if (openingLine < 0 || !normalized.endsWith("\"\"\"")) {
                throw input.errorAt("Malformed text block", span);
            }
            return normalized.substring(openingLine + 1, normalized.length() - 3)
                    .stripIndent().translateEscapes();
        } catch (IllegalArgumentException exception) {
            throw input.errorAt("Invalid literal escape: " + exception.getMessage(), span);
        }
    }

    private static List<NumericForm> loadForms() {
        InputStream stream = JavaLiteralParser.class.getResourceAsStream("/org/jmouse/java/parser/numeric-literals.tsv");
        if (stream == null) {
            throw new IllegalStateException("Missing numeric literal grammar");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().filter(line -> !line.isBlank() && !line.startsWith("#")).map(line -> {
                String[] columns = line.split("\t", -1);
                if (columns.length != 3) {
                    throw new IllegalArgumentException("Malformed numeric literal grammar entry");
                }
                return new NumericForm(NumericKind.valueOf(columns[0]), Integer.parseInt(columns[1]),
                        Pattern.compile(columns[2]));
            }).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read numeric literal grammar", exception);
        }
    }
}
