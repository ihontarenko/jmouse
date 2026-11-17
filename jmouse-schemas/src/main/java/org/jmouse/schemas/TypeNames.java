package org.jmouse.schemas;

import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;

/**
 * A defined type named as a reader finds it in its source — {@code PcbDrawing.Track}, its name inside its package —, not by the
 * generator's own spelling of its generics.
 */
final class TypeNames implements SchemaDefinitionNamingStrategy {

    @Override
    public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext context) {
        return TypeSchemas.nameOf(key.getType().getErasedType());
    }
}
