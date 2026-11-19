package org.jmouse.java.parser.lexer;

import java.util.Optional;
import org.jmouse.java.model.version.JavaFeatureId;

/** Implementable by another enum without modifying JavaKeyword. */
public interface JavaKeywordDefinition {
    String spelling();

    JavaKeywordCategory category();

    default Optional<JavaFeatureId> typeNameRestriction() {
        return Optional.empty();
    }
}
