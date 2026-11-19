package org.jmouse.java.parser.grammar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jmouse.el.parser.expression.Associativity;

/** Java spelling and binding powers are grammar data, not evaluator operators. */
public final class JavaOperatorCatalog {
    public enum Kind { BINARY, ASSIGNMENT, CONDITIONAL, TYPE_TEST, PREFIX, POSTFIX, DELIMITER }
    public record Definition(Kind kind, String identity, String spelling, int precedence,
                             Associativity associativity) {}
    private final List<Definition> definitions;
    private final List<String> spellings;

    public JavaOperatorCatalog(List<Definition> definitions) {
        this.definitions = List.copyOf(definitions);
        if (this.definitions.isEmpty()) {
            throw new IllegalArgumentException("An operator catalog cannot be empty");
        }
        Set<String> identities = new HashSet<>();
        for (Definition definition : this.definitions) {
            if (definition.spelling().isEmpty() || definition.precedence() < 0
                    || !identities.add(definition.kind() + ":" + definition.spelling())) {
                throw new IllegalArgumentException("Invalid or duplicate Java operator: " + definition);
            }
        }
        spellings = this.definitions.stream().map(Definition::spelling).distinct()
                .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    public Definition find(JavaParsingInput input, Set<Kind> kinds) {
        String spelling = spellings.stream().filter(input::matches).findFirst().orElse(null);
        if (spelling == null) {
            return null;
        }
        return definitions.stream().filter(definition -> definition.spelling().equals(spelling)
                && kinds.contains(definition.kind())).findFirst().orElse(null);
    }

    public static JavaOperatorCatalog standard() {
        InputStream stream = JavaOperatorCatalog.class.getResourceAsStream("/org/jmouse/java/parser/operators.tsv");
        if (stream == null) {
            throw new IllegalStateException("Missing Java operator grammar");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return new JavaOperatorCatalog(reader.lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#")).map(line -> {
                        String[] columns = line.split("\t", -1);
                        if (columns.length != 5) {
                            throw new IllegalArgumentException("Malformed Java operator grammar entry");
                        }
                        return new Definition(Kind.valueOf(columns[0]), columns[1], columns[2],
                                Integer.parseInt(columns[3]), Associativity.valueOf(columns[4]));
                    }).toList());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Java operators", exception);
        }
    }
}
