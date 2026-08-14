package org.jmouse.ai.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One turn, sent whole.
 *
 * <p>Stateless on purpose: there is no conversation on this side of the port. The caller holds the
 * growing message array and sends it again each round, which is what lets a model be swapped between
 * two rounds of the same conversation and what keeps this module free of any storage question.
 *
 * <p>⚠️ <strong>No {@code application} field</strong>, which the implementation this was learned from
 * carried. That was the key its settings were <em>looked up</em> by, not part of the call, and
 * conflating the two is precisely why that gateway could not be used as a library — every caller had
 * to name itself to something that had no business knowing. Looking settings up is
 * {@link ProviderSettingsSource}'s, and how it decides is nobody else's business.
 *
 * @param system   standing instructions, sent outside the conversation; null or blank for none
 * @param messages the conversation so far, in the provider-neutral role/content shape
 * @param tools    what the model may call, or empty for a conversation with no tools
 */
public record ChatRequest(String system, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {

    public ChatRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        tools    = tools    == null ? List.of() : List.copyOf(tools);
    }

    public static ChatRequest of(List<Map<String, Object>> messages) {
        return new ChatRequest(null, messages, List.of());
    }

    public ChatRequest withSystem(String system) {
        return new ChatRequest(system, messages, tools);
    }

    public ChatRequest withTools(List<Map<String, Object>> tools) {
        return new ChatRequest(system, messages, tools);
    }

    /** The same request one turn further on — what a loop does between rounds. */
    public ChatRequest withMessageAppended(Map<String, Object> message) {
        List<Map<String, Object>> grown = new ArrayList<>(messages);
        grown.add(message);

        return new ChatRequest(system, grown, tools);
    }

    public boolean hasSystem() {
        return system != null && !system.isBlank();
    }

    public boolean hasTools() {
        return !tools.isEmpty();
    }
}
