package org.jmouse.access;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Everything a capability grant can be about, in one list — {@code PermissionCatalog} for the
 * entitlement axis.
 *
 * <p>Without one, <em>"what can a plan contain?"</em> is answered by whichever screen is being written
 * that week, and the answers differ per screen. With one, the grant resolution, the plan bundles, the
 * policy document and the startup validator all speak the same vocabulary and cannot disagree about
 * what exists.
 *
 * <p>⚠️ <strong>The library never populates this.</strong> A product registers it, and a product with
 * completely different layers registers completely different entries — which is the whole bargain:
 * adopting the axis costs a vocabulary and one store, not a fork.
 */
public final class CapabilityCatalog {

    private final Map<String, CapabilityDefinition> byKey;

    public CapabilityCatalog(Collection<CapabilityDefinition> capabilities) {
        Map<String, CapabilityDefinition> index = new LinkedHashMap<>();

        for (CapabilityDefinition definition : capabilities) {
            CapabilityDefinition existing = index.put(definition.key(), definition);

            if (existing != null) {
                throw new IllegalArgumentException(
                        "Capability '" + definition.key() + "' is declared twice. Two definitions of one "
                        + "key means whichever screen reads it last decides what it means.");
            }
        }

        this.byKey = Map.copyOf(index);
    }

    /** A catalogue holding nothing — a product that has not adopted the axis. */
    public static CapabilityCatalog empty() {
        return new CapabilityCatalog(List.of());
    }

    public boolean contains(String capability) {
        return byKey.containsKey(capability);
    }

    public Optional<CapabilityDefinition> find(String capability) {
        return Optional.ofNullable(byKey.get(capability));
    }

    /**
     * One capability, or a failure naming what the catalogue does carry.
     *
     * <p>For write paths — issuing a grant, binding a document — where an unknown key is a mistake to
     * refuse rather than a state to resolve around.
     */
    public CapabilityDefinition require(String capability) {
        return find(capability).orElseThrow(() -> new IllegalArgumentException(
                "No capability '" + capability + "'. A grant can only be about something this catalogue "
                + "names; known capabilities: " + String.join(", ", all()) + "."));
    }

    public Set<String> all() {
        return byKey.keySet();
    }

    public Collection<CapabilityDefinition> definitions() {
        return byKey.values();
    }

    /** The ones that carry a number — what a bundle expresses as an allowance. */
    public List<CapabilityDefinition> metered() {
        return byKey.values().stream().filter(CapabilityDefinition::isMetered).toList();
    }

    /**
     * The ones closed until something grants them.
     *
     * <p>⚠️ What an installation is actually selling. A catalogue where everything is paid has no free
     * tier to offer; one where nothing is means the axis is never exercised until launch day.
     */
    public List<CapabilityDefinition> paid() {
        return byKey.values().stream().filter(CapabilityDefinition::paid).toList();
    }

    public boolean isEmpty() {
        return byKey.isEmpty();
    }

    /** What a failure message prints when it has to say what the catalogue holds. */
    public String describe() {
        return byKey.values().stream()
                .map(definition -> definition.key() + " (" + definition.kind() + ")")
                .collect(Collectors.joining(", "));
    }
}
