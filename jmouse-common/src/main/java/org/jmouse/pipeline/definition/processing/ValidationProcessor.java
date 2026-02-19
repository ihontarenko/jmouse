package org.jmouse.pipeline.definition.processing;

import org.jmouse.common.pipeline.definition.model.*;
import org.jmouse.core.Verify;
import org.jmouse.pipeline.definition.model.ChainDefinition;
import org.jmouse.pipeline.definition.model.LinkDefinition;
import org.jmouse.pipeline.definition.model.LinkProperties;
import org.jmouse.pipeline.definition.model.PipelineDefinition;

import java.util.Map;

public final class ValidationProcessor implements DefinitionPostProcessor {

    @Override
    public PipelineDefinition process(PipelineDefinition pipelineDefinition) {
        Verify.notEmpty(pipelineDefinition.name(), "pipeline.name");

        for (ChainDefinition chain : pipelineDefinition.chains().values()) {
            validateChain(pipelineDefinition, chain);
        }

        return pipelineDefinition;
    }

    private void validateChain(PipelineDefinition def, ChainDefinition chain) {
        Verify.notEmpty(chain.name(), "chain.name");
        Verify.notEmpty(chain.initial(), "chain.initial (%s)".formatted(chain.name()));

        Map<String, LinkDefinition> links = chain.links();
        if (links == null || links.isEmpty()) {
            throw new DefinitionProcessingException("Chain '%s' has no links".formatted(chain.name()));
        }

        if (!links.containsKey(chain.initial())) {
            throw new DefinitionProcessingException(
                    "Chain '%s' initial link not found: %s".formatted(chain.name(), chain.initial()));
        }

        for (LinkDefinition link : links.values()) {
            validateLink(chain, link, links);
        }
    }

    private void validateLink(ChainDefinition chain, LinkDefinition link, Map<String, LinkDefinition> all) {
        Verify.notEmpty(link.name(), "link.name (%s)".formatted(chain.name()));

        if (link.processor() == null) {
            throw new DefinitionProcessingException(
                    "Link '%s:%s' has no processor definition".formatted(
                            chain.name(), link.name())
            );
        }
        Verify.notEmpty(link.processor().className(),
                "processor.className (%s:%s)".formatted(chain.name(), link.name()));

        LinkProperties props = link.properties();
        if (props == null) {
            return;
        }

        // transitions -> existing links
        props.transitions().forEach((returnCode, nextLink) -> {
            Verify.notEmpty(returnCode, "transition.return (%s:%s)".formatted(chain.name(), link.name()));
            Verify.notEmpty(nextLink, "transition.link (%s:%s)".formatted(chain.name(), link.name()));
            if (!all.containsKey(nextLink)) {
                throw new DefinitionProcessingException(
                        "Transition target not found: chain='%s', from='%s', return='%s', to='%s'".formatted(
                                chain.name(), link.name(), returnCode, nextLink)
                );
            }
        });

        // fallback -> existing link
        if (props.fallback() != null && !props.fallback().isBlank() && !all.containsKey(props.fallback())) {
            throw new DefinitionProcessingException(
                    "Fallback target not found: chain='%s', link='%s', fallback='%s'".formatted(
                            chain.name(), link.name(), props.fallback())
            );
        }
    }

}
