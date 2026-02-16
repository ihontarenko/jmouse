package org.jmouse.common.pipeline.definition;

import org.jmouse.common.pipeline.definition.processing.DefinitionPostProcessor;
import org.jmouse.common.pipeline.definition.processing.DefinitionProcessorChain;
import org.jmouse.common.pipeline.definition.processing.NormalizeProcessor;
import org.jmouse.common.pipeline.definition.processing.ValidationProcessor;

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
