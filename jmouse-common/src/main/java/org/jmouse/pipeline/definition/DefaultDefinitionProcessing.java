package org.jmouse.pipeline.definition;

import org.jmouse.pipeline.definition.processing.DefinitionPostProcessor;
import org.jmouse.pipeline.definition.processing.DefinitionProcessorChain;
import org.jmouse.pipeline.definition.processing.NormalizeProcessor;
import org.jmouse.pipeline.definition.processing.ValidationProcessor;

import java.util.List;

public final class DefaultDefinitionProcessing {

    private DefaultDefinitionProcessing() {}

    public static DefinitionPostProcessor defaults() {
        return new DefinitionProcessorChain(
                List.of(
                        new NormalizeProcessor(),
                        new ValidationProcessor()
                )
        );
    }
}
