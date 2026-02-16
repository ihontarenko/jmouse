package org.jmouse.common.pipeline;

import org.jmouse.common.pipeline.context.PipelineContext;
import org.jmouse.core.context.result.MutableResultContext;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public record PipelineProcessorChain(
        String initial,
        Map<String, PipelineProcessor> processors,
        Map<String, ProcessorProperties> properties
) implements PipelineChain {

    private static final Logger LOGGER = getLogger(PipelineProcessorChain.class);

    private static final PipelineProcessor DEFAULT_FALLBACK = new FallbackProcessor();

    @Override
    public void proceed(PipelineContext context) throws Exception {
        String currentLink = initial;

        PipelineResult       previous = null;
        Map<String, Boolean> visited  = new HashMap<>();

        LOGGER.info("[PIPELINE-CHAIN]: INITIAL PROCESS {}", initial);

        while (currentLink != null) {

            PipelineProcessor   processor           = processors.get(currentLink);
            ProcessorProperties processorProperties = properties.get(currentLink);

            if (processor == null) {
                throw new MissingProcessorLinkException(
                        "No processor with the link name '%s' was located. Check pipeline definition."
                                .formatted(currentLink));
            }

            if (visited.containsKey(currentLink)) {
                throw new CyclicInvocationDetected(
                        "Processor '%s' encountered a recursive call".formatted(currentLink));
            }

            PipelineResult result;

            try {
                result = processor.process(context, context.getArgumentsContext(), previous);
            } catch (Exception ex) {

                if (context.getValue("EXCEPTION") == null) {
                    context.setValue("EXCEPTION", ex);
                }

                handleFallback(context, processorProperties, ex);
                break;
            }

            previous = result;

            // if you already have a stop flag in context — respect it
            if (context.isProcessingStopped()) {
                LOGGER.info("[PIPELINE-CHAIN]: STOPPED BY CONTEXT FLAG at '{}'", currentLink);
                break;
            }

            if (result == null) {
                throw new InvalidProcessorReturnException(
                        "Processor '%s' returned null PipelineResult".formatted(currentLink));
            }

            if (result.error() != null) {
                Throwable err = result.error();
                Exception ex = (err instanceof Exception e) ? e : new PipelineRuntimeException(err);
                handleFallback(context, processorProperties, ex);
                break;
            }

            if (result.stop()) {
                LOGGER.info("[PIPELINE-CHAIN]: STOPPED BY RESULT at '{}'", currentLink);
                break;
            }

            visited.put(currentLink, true);

            if (result.hasJump()) {
                String jumpTo = result.jumpTo();
                LOGGER.info("[PIPELINE-CHAIN]: JUMP '{}' -> '{}'", currentLink, jumpTo);
                currentLink = jumpTo;
                continue;
            }

            String codeKey = result.codeKey();
            if (codeKey == null) {
                throw new InvalidProcessorReturnException(
                        "Processor '%s' returned PipelineResult with null code and no jump"
                                .formatted(currentLink));
            }

            String next = (processorProperties == null) ? null : processorProperties.getNext(codeKey);

            LOGGER.info("[PIPELINE-CHAIN]: RETURN '{}' -> NEXT '{}'", codeKey, next);

            if (next == null) {
                throw new InvalidProcessorReturnException(
                        "No transition mapping for return '%s' at link '%s'".formatted(codeKey, currentLink));
            }

            currentLink = next;
        }
    }

    private void handleFallback(
            PipelineContext context, ProcessorProperties properties, Exception exception) throws Exception {

        PipelineProcessor    fallback = DEFAULT_FALLBACK;
        MutableResultContext result   = context.getResultContext();

        // If link-level fallback points to another processor link - use it
        if (properties != null && properties.fallback() != null) {
            PipelineProcessor processor = processors.get(properties.fallback());
            if (processor != null) {
                fallback = processor;
            }
        }

        // record error in result context (your real API)
        result.addError("EXCEPTION", exception.getMessage());

        // keep your existing exception propagation style
        context.setValue("EXCEPTION", exception);
        context.setValue(Throwable.class, exception);

        // execute fallback (it may throw)
        fallback.process(context);

        LOGGER.error("[PIPELINE-CHAIN]: ERROR: '{}', FALLBACK: '{}'",
                     exception.getMessage(), fallback.getClass().getName());
    }
}
