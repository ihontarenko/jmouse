package org.jmouse.common.pipeline.smoke;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jmouse.common.pipeline.*;
import org.jmouse.common.pipeline.context.DefaultPipelineContext;
import org.jmouse.common.pipeline.context.PipelineContext;
import org.jmouse.common.pipeline.definition.DefaultDefinitionProcessing;
import org.jmouse.common.pipeline.definition.loading.ClasspathSource;
import org.jmouse.common.pipeline.definition.loading.DefinitionLoader;
import org.jmouse.common.pipeline.definition.loading.dto.XmlDTO2DefinitionMapper;
import org.jmouse.common.pipeline.definition.loading.readers.XmlDefinitionReader;
import org.jmouse.common.pipeline.definition.model.PipelineDefinition;
import org.jmouse.common.pipeline.runtime.DefaultPipelineCompiler;
import org.jmouse.common.pipeline.runtime.PipelineCompiler;
import org.jmouse.core.context.mutable.MutableArgumentsContext;
import org.jmouse.core.proxy.DefaultProxyFactory;

import java.util.List;

public final class PipelineXmlSmoke {

    public static void main(String[] args) throws Exception {

        DefinitionLoader loader = new DefinitionLoader(
                List.of(
                        new XmlDefinitionReader(new XmlMapper(), new XmlDTO2DefinitionMapper())
                ),
                DefaultDefinitionProcessing.defaults()
        );

        PipelineDefinition def = loader.load(new ClasspathSource("/pipeline/smoke-pipeline.xml"));

        // 2) Normalize + validate (your processing pipeline)
        // def = DefaultDefinitionProcessing.defaults().process(def);

        // 3) Compile
        PipelineCompiler compiler = new DefaultPipelineCompiler(
                new PipelineProcessorFactory(),
                new DefaultProxyFactory()
        );

        // 4) Run
        PipelineManager manager = new PipelineManager(def, compiler);

        PipelineContext ctx = new DefaultPipelineContext();
        manager.run("smoke-xml", ctx);

        // 5) Inspect outputs
        System.out.println("returnValue = " + ctx.getResultContext().getReturnValue());
        System.out.println("args.lastPayload = " + ctx.getArgumentsContext().getValue("lastPayload"));
        System.out.println("args.message = " + ctx.getArgumentsContext().getValue("message"));
    }

    public enum Code { NEXT }

    // ---------- Processors ----------

    public static final class XmlStartProcessor implements PipelineProcessor {

        public String message; // injected via <parameter name="message" .../>

        @Override
        public PipelineResult process(PipelineContext context,
                                      MutableArgumentsContext arguments,
                                      PipelineResult previous) {

            // show that XML parameter injection worked
            arguments.setValue("message", message);

            // put something as "output"
            context.getResultContext().setReturnValue("start");

            return PipelineResult.of(Code.NEXT, "payload-1");
        }
    }

    public static final class XmlJumpProcessor implements PipelineProcessor {

        @Override
        public PipelineResult process(PipelineContext context,
                                      MutableArgumentsContext arguments,
                                      PipelineResult previous) {

            // previous comes from start
            arguments.setValue("lastPayload", previous == null ? null : previous.payload());

            // demonstrate forced jump: transitions will be ignored
            return PipelineResult.jump("finish");
        }
    }

    public static final class XmlFinishProcessor implements PipelineProcessor {

        @Override
        public PipelineResult process(PipelineContext context,
                                      MutableArgumentsContext arguments,
                                      PipelineResult previous) {

            context.getResultContext().setReturnValue("finished");

            // clean terminal signal (no transitions required)
            return PipelineResult.finish();
        }
    }

    /** Optional fallback processor (if you wire <fallback link="fallback"/> somewhere). */
    public static final class XmlFallbackProcessor implements PipelineProcessor {

        @Override
        public PipelineResult process(PipelineContext context,
                                      MutableArgumentsContext arguments,
                                      PipelineResult previous) {

            Throwable ex = context.getValue("EXCEPTION");
            context.getResultContext().addError("SMOKE_FALLBACK", ex == null ? "unknown" : ex.getMessage());
            context.getResultContext().setReturnValue("fallback-executed");

            return PipelineResult.finish();
        }
    }
}
