package org.jmouse.pipeline.smoke;

import org.jmouse.pipeline.PipelineManager;
import org.jmouse.pipeline.PipelineProcessor;
import org.jmouse.pipeline.PipelineProcessorFactory;
import org.jmouse.pipeline.PipelineResult;
import org.jmouse.pipeline.context.DefaultPipelineContext;
import org.jmouse.pipeline.context.PipelineContext;
import org.jmouse.pipeline.definition.DefaultDefinitionProcessing;
import org.jmouse.pipeline.definition.dsl.PipelineDefinitions;
import org.jmouse.pipeline.definition.model.PipelineDefinition;
import org.jmouse.pipeline.runtime.DefaultPipelineCompiler;
import org.jmouse.pipeline.runtime.PipelineCompiler;
import org.jmouse.core.context.mutable.MutableArgumentsContext;
import org.jmouse.core.proxy.DefaultProxyFactory;

public final class PipelineSmoke {

    public static void main(String[] args) throws Exception {

        PipelineDefinition def = PipelineDefinitions.pipeline("DEFAULT", p -> {
            p.chain("smoke", c -> {
                c.initial("start");

                c.link("start")
                        .processor(StartProcessor.class)
                        .onReturn(SmokeCode.NEXT.name(), "jump");

                c.link("jump")
                        .processor(JumpProcessor.class);
                // JumpProcessor returns PipelineResult.jump("finish")

                c.link("finish")
                        .processor(FinishProcessor.class);
            });
        });

        def = DefaultDefinitionProcessing.defaults().process(def);

        PipelineCompiler compiler = new DefaultPipelineCompiler(
                new PipelineProcessorFactory(),
                new DefaultProxyFactory()
        );

        PipelineManager manager = new PipelineManager(def, compiler);

        PipelineContext ctx = new DefaultPipelineContext();
        manager.run("smoke", ctx);

        System.out.println("returnValue = " + ctx.getResultContext().getReturnValue());
        System.out.println("args.prevCode = " + ctx.getArgumentsContext().getValue("prevCode"));
        System.out.println("args.lastPayload = " + ctx.getArgumentsContext().getValue("lastPayload"));
    }

    public static class StartProcessor implements PipelineProcessor {
        @Override
        public PipelineResult process(PipelineContext context,
                                      MutableArgumentsContext arguments,
                                      PipelineResult previous) {

            // Your ResultContext is execution-output only
            context.getResultContext().setReturnValue("start");

            // show previous usage: store it in arguments if you want
            if (previous != null) {
                arguments.setArgument("prevCode", previous.codeKey());
            }

            return PipelineResult.of(SmokeCode.NEXT, "payload-1");
        }
    }

    public static class JumpProcessor implements PipelineProcessor {
        @Override
        public PipelineResult process(PipelineContext context,
                                      MutableArgumentsContext arguments,
                                      PipelineResult previous) {

            arguments.setValue("prevCode", previous == null ? null : previous.codeKey());
            arguments.setValue("lastPayload", previous == null ? null : previous.payload());

            return PipelineResult.jump("finish");
        }
    }

    public static class FinishProcessor implements PipelineProcessor {
        @Override
        public PipelineResult process(PipelineContext context,
                                      MutableArgumentsContext arguments,
                                      PipelineResult previous) {

            context.getResultContext().setReturnValue("finished");

            return PipelineResult.finish();
        }
    }


}
