package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.PipelineContext;
import org.jmouse.common.pipeline.definition.model.PipelineDefinition;
import org.jmouse.common.pipeline.runtime.PipelineCompiler;

import java.util.HashMap;
import java.util.Map;

public final class PipelineManager {

    private final PipelineCompiler           compiler;
    private final PipelineDefinition         definition;
    private final Map<String, PipelineChain> cache = new HashMap<>();

    public PipelineManager(PipelineDefinition definition, PipelineCompiler compiler) {
        this.definition = definition;
        this.compiler = compiler;
    }

    public PipelineChain chain(String name) {
        return cache.computeIfAbsent(name,
                n -> compiler.compile(definition, n));
    }

    public void run(String chainName, PipelineContext context) throws Exception {
        context.getResultContext().clear();
        chain(chainName).proceed(context);
    }
}

