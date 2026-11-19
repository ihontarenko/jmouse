package org.jmouse.java.parser.lexer;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jmouse.java.model.version.JavaFeatureId;

/** Immutable vocabulary; third-party definitions are copied before a lexer can observe them. */
public final class JavaKeywordCatalog {
    private record Definition(String spelling, JavaKeywordCategory category,
                              Optional<JavaFeatureId> typeNameRestriction) implements JavaKeywordDefinition {
        private Definition {
            Objects.requireNonNull(spelling, "spelling");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(typeNameRestriction, "typeNameRestriction");
            if (spelling.isBlank() || !spelling.equals(spelling.strip())) {
                throw new IllegalArgumentException("A keyword needs an exact nonblank spelling");
            }
        }

        private static Definition copy(JavaKeywordDefinition definition) {
            return new Definition(definition.spelling(), definition.category(), definition.typeNameRestriction());
        }
    }

    private final Map<String, Definition> definitions;

    public JavaKeywordCatalog(Collection<? extends JavaKeywordDefinition> keywords) {
        Map<String, Definition> definitions = new LinkedHashMap<>();
        for (JavaKeywordDefinition keyword : keywords) {
            Definition definition = Definition.copy(keyword);
            if (definitions.putIfAbsent(definition.spelling(), definition) != null) {
                throw new IllegalArgumentException("Duplicate keyword spelling: " + definition.spelling());
            }
        }
        this.definitions = Map.copyOf(definitions);
    }

    public static JavaKeywordCatalog standard() {
        return Standard.INSTANCE;
    }

    public static JavaKeywordDefinition snapshotDefinition(JavaKeywordDefinition keyword) {
        if (keyword instanceof JavaKeyword || keyword instanceof Definition) {
            return keyword;
        }
        return Definition.copy(Objects.requireNonNull(keyword, "keyword"));
    }

    public Optional<JavaKeywordDefinition> find(String spelling) {
        return Optional.ofNullable(definitions.get(spelling));
    }

    public boolean isReserved(String spelling) {
        Definition definition = definitions.get(spelling);
        return definition != null && definition.category() == JavaKeywordCategory.RESERVED;
    }

    public List<JavaKeywordDefinition> definitions() {
        return definitions.values().stream().map(JavaKeywordDefinition.class::cast)
                .sorted(Comparator.comparing(JavaKeywordDefinition::spelling)).toList();
    }

    public JavaKeywordCatalog extend(Collection<? extends JavaKeywordDefinition> additions) {
        List<JavaKeywordDefinition> combined = new ArrayList<>(definitions());
        combined.addAll(additions);
        return new JavaKeywordCatalog(combined);
    }

    public JavaKeywordCatalog replace(JavaKeywordDefinition expected, JavaKeywordDefinition replacement) {
        Definition previous = definitions.get(expected.spelling());
        Definition updated = Definition.copy(replacement);
        if (!Objects.equals(previous, Definition.copy(expected)) || !expected.spelling().equals(updated.spelling())) {
            throw new IllegalArgumentException("Keyword replacement does not match the registered spelling and definition");
        }
        return new JavaKeywordCatalog(definitions.values().stream()
                .map(definition -> definition.spelling().equals(expected.spelling()) ? updated : definition).toList());
    }

    private static final class Standard {
        private static final JavaKeywordCatalog INSTANCE = new JavaKeywordCatalog(Arrays.asList(JavaKeyword.values()));
    }
}
