package org.jmouse.java.bytecode.vocabulary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public final class JvmVocabulary {
    private static final Map<JvmOpcode, JvmOpcodeDefinition> OPCODES =
            new EnumMap<>(JvmOpcode.class);
    private static final JvmOpcode[] BY_CODE = new JvmOpcode[1 << Byte.SIZE];
    private static final Map<JvmAttributeKind, JvmAttributeDefinition> ATTRIBUTES =
            new EnumMap<>(JvmAttributeKind.class);
    private static final Map<String, Map<String, JvmTokenRange>> TOKENS = new HashMap<>();
    private static final Properties NAMESPACES = new Properties();

    static {
        for (String[] row : rows("jvm-opcodes.tsv")) {
            JvmOpcode opcode = JvmOpcode.valueOf(row[1]);
            int code = Integer.parseInt(row[0]);
            if (BY_CODE[code] != null) {
                throw new IllegalStateException("Duplicate JVM opcode");
            }
            BY_CODE[code] = opcode;
            OPCODES.put(
                    opcode,
                    new JvmOpcodeDefinition(
                            code,
                            row[2],
                            JvmOpcodeClassification.valueOf(row[3]),
                            JvmOperandEncoding.valueOf(row[4]),
                            row[5],
                            Integer.parseInt(row[6]),
                            Integer.parseInt(row[7]),
                            Integer.parseInt(row[8]),
                            Integer.parseInt(row[9])));
        }
        if (Arrays.stream(BY_CODE).anyMatch(Objects::isNull)) {
            throw new IllegalStateException("Incomplete JVM opcode classification");
        }
        for (String[] row : rows("jvm-tokens.tsv")) {
            String[] range = row[2].split("\\.\\.", -1);
            int minimum = Integer.decode(range[0]);
            int maximum = range.length == 1 ? minimum : Integer.decode(range[1]);
            if (TOKENS.computeIfAbsent(row[0], ignored -> new HashMap<>())
                            .putIfAbsent(row[1], new JvmTokenRange(minimum, maximum))
                    != null) {
                throw new IllegalStateException("Duplicate JVM token");
            }
        }
        for (String[] row : rows("jvm-attributes.tsv")) {
            String[] version = row[3].split("\\.");
            ATTRIBUTES.put(
                    JvmAttributeKind.valueOf(row[0]),
                    new JvmAttributeDefinition(
                            row[1],
                            Integer.parseInt(version[0]),
                            Integer.parseInt(version[1]),
                            row[2],
                            row[4],
                            Arrays.stream(row[5].split(","))
                                    .map(JvmAttributeContext::valueOf)
                                    .collect(Collectors.toUnmodifiableSet()),
                            JvmAttributeMultiplicity.valueOf(row[6])));
        }
        try (InputStream input = open("jvm-namespaces.properties")) {
            NAMESPACES.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private JvmVocabulary() {}

    public static JvmOpcode opcode(int code) {
        if (code < 0 || code >= BY_CODE.length) {
            throw new IllegalArgumentException("Opcode is not an unsigned byte");
        }
        return BY_CODE[code];
    }

    public static JvmOpcodeDefinition opcode(JvmOpcode opcode) {
        return OPCODES.get(opcode);
    }

    public static JvmAttributeDefinition attribute(JvmAttributeKind kind) {
        return ATTRIBUTES.get(kind);
    }

    public static String namespace(Class<?> type) {
        String namespace = NAMESPACES.getProperty(type.getSimpleName());
        if (namespace == null) {
            throw new IllegalArgumentException("Unregistered token namespace: " + type.getName());
        }
        return namespace;
    }

    public static JvmTokenRange range(JvmToken token) {
        JvmTokenRange range = TOKENS.getOrDefault(token.namespace(), Map.of()).get(token.name());
        if (range == null) {
            throw new IllegalArgumentException("Unregistered JVM token: " + token);
        }
        return range;
    }

    public static <T extends Enum<T> & JvmToken> T token(Class<T> type, int code) {
        return Arrays.stream(type.getEnumConstants())
                .filter(token -> token.minimum() <= code && code <= token.maximum())
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown " + type.getSimpleName() + " code: " + code));
    }

    private static InputStream open(String name) {
        InputStream input = JvmVocabulary.class.getResourceAsStream(name);
        if (input == null) {
            throw new IllegalStateException("Missing JVM vocabulary resource: " + name);
        }
        return input;
    }

    private static List<String[]> rows(String name) {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(open(name), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("\\t", -1))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read JVM vocabulary: " + name, exception);
        }
    }
}
