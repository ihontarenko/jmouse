package org.jmouse.pipeline;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNullElseGet;

public record ProcessorProperties(
        Map<String, String> transitions,
        Map<String, String> configurations,
        String fallback
) {

    public String configuration(Configuration parameter) {
        return (String) requireNonNullElseGet(configurations(), HashMap::new)
                .get(parameter.parameter);
    }

    public String getNext(String returnCode) {
        return (String) requireNonNullElseGet(transitions(), HashMap::new)
                .get(returnCode);
    }

    public boolean isTerminal() {
        return Boolean.parseBoolean(configuration(Configuration.TERMINAL));
    }

    public enum Configuration {
        MAX_PROCESSOR_CALLS("max-processor-calls"),
        TERMINAL("terminal");

        private final String parameter;

        Configuration(String parameter) {
            this.parameter = parameter;
        }
    }
}
