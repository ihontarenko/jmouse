package org.jmouse.common.pipeline.definition.loading.dto.xml;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.*;

@SuppressWarnings({"unused"})
public record XmlRootDTO(
        @JacksonXmlProperty(localName = "name")
        String name,
        Map<String, Chain> chains
) {

    public XmlRootDTO {
        chains = new LinkedHashMap<>();
    }

    @JsonAnySetter
    public void addProcessChain(String ignoredKey, Chain chain) {
        chains.put(chain.name(), chain);
    }

    public record Chain(
            @JacksonXmlProperty(localName = "name")
            String name,
            @JacksonXmlProperty(localName = "initial")
            String initial,
            Map<String, Link> links
    ) {
        public Chain {
            links = new LinkedHashMap<>();
        }

        @JsonAnySetter
        public void addProcessorLink(String ignoredKey, Link link) {
            links.put(link.name(), link);
        }
    }

    public record Link(
            @JacksonXmlProperty(localName = "name")
            String name,
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "processor")
            Processor processor,
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "properties")
            Properties properties
    ) {}

    public record Processor(
            @JacksonXmlProperty(isAttribute = true, localName = "class")
            String className,
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "parameter")
            List<Parameter> parameters
    ) {}

    public record Properties(
            Map<String, String> configuration,
            Map<String, String> transitions,
            Fallback fallback
    ) {
        public Properties {
            transitions = new LinkedHashMap<>();
            configuration = new LinkedHashMap<>();
        }

        @JsonAnySetter
        public void addTransition(String ignoredKey, Transition transition) {
            transitions.put(transition.returnCode(), transition.link());
        }
    }

    public record Transition(
            @JacksonXmlProperty(isAttribute = true, localName = "return")
            String returnCode,

            @JacksonXmlProperty(isAttribute = true, localName = "link")
            String link
    ) {}

    public record Fallback(
            @JacksonXmlProperty(isAttribute = true, localName = "link")
            String link
    ) {}

    public record Parameter(
            @JacksonXmlProperty(isAttribute = true, localName = "name")
            String name,
            @JacksonXmlProperty(isAttribute = true, localName = "value")
            String value,
            @JacksonXmlProperty(isAttribute = true, localName = "resolver")
            String resolver,
            @JacksonXmlProperty(isAttribute = true, localName = "converter")
            String converter
    ) {}
}
