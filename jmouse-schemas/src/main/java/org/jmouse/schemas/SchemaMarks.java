package org.jmouse.schemas;

import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

import java.util.function.Consumer;

/**
 * What an owner marks on a schema beside what the type says — a settings window's titles, units and bounds —, through the
 * generator's own configuration; what it finds missing is noted with the rest of what the schema cannot say.
 */
@FunctionalInterface
public interface SchemaMarks {

    /** Nothing beside the type. */
    SchemaMarks NONE = (configuration, undescribed) -> {
    };

    void mark(SchemaGeneratorConfigBuilder configuration, Consumer<String> undescribed);
}
