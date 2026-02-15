package org.jmouse.validator.dynamic;

import java.util.LinkedHashMap;
import java.util.Map;

public interface ConstraintCatalog {

    static Builder builder() {
        return new Builder();
    }

    ConstraintDefinition get(String idOrAlias);

    final class Builder {

        private final Map<String, ConstraintDefinition> idMap = new LinkedHashMap<>();
        private final Map<String, ConstraintDefinition> alias = new LinkedHashMap<>();

        public Builder register(ConstraintDefinition definition) {
            idMap.put(definition.id(), definition);

            for (String a : definition.aliases()) {
                alias.put(a, definition);
            }

            return this;
        }

        public ConstraintCatalog build() {
            var id    = Map.copyOf(idMap);
            var alias = Map.copyOf(this.alias);
            return key -> {
                ConstraintDefinition definition = id.get(key);
                return definition != null ? definition : alias.get(key);
            };
        }
    }
}
