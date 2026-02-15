package org.jmouse.validator.constraint.registry;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.validator.constraint.model.*;

import java.util.ArrayList;
import java.util.List;

public final class ConstraintSchemaCompiler {

    public ConstraintSchema compile(ConstraintSchema schema) {
        if (schema == null) {
            return null;
        }

        List<CompiledFieldRules> compiledFields = new ArrayList<>();

        for (FieldRules field : schema.fields()) {
            PropertyPath propertyPath = field.propertyPath();
            compiledFields.add(new CompiledFieldRules(
                    field.path(),
                    propertyPath,
                    List.copyOf(field.rules())
            ));
        }

        return new CompiledConstraintSchema(schema.name(), List.copyOf(compiledFields));
    }
}
