package org.jmouse.ai.preferences;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Every setting this application declares, and the gate that stops two of them being the same one.
 *
 * <p>The small sibling of {@code ToolCatalog}, and for the same reason: what may be addressed is fixed
 * at startup, so a name that is wrong fails beside the declaration rather than as a row nothing reads
 * or a request nothing answers.
 *
 * <p>⚠️ <strong>A name nobody declared is refused rather than stored.</strong> A free key-value table
 * would accept a typo happily, and the setting the typo was meant to be would go on quietly using its
 * default — which is the failure that has no symptom until somebody asks why an edit did nothing.
 */
public final class PreferenceCatalog {

    private final Map<String, PreferenceDefinition> declaredByName;

    private PreferenceCatalog(Map<String, PreferenceDefinition> declaredByName) {
        this.declaredByName = Map.copyOf(declaredByName);
    }

    /** An application that declares nothing — every read refuses, which is what "undeclared" means. */
    public static PreferenceCatalog empty() {
        return new PreferenceCatalog(Map.of());
    }

    /**
     * @throws IllegalStateException where two declarations share a name — which of them won would
     *                               otherwise depend on the order beans arrived in
     */
    public static PreferenceCatalog of(Collection<PreferenceDefinition> declarations) {
        List<String> duplicated = declarations.stream()
                .collect(Collectors.groupingBy(PreferenceDefinition::name, Collectors.counting()))
                .entrySet().stream()
                .filter(counted -> counted.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!duplicated.isEmpty()) {
            throw new IllegalStateException(
                    "These preferences are declared more than once: " + String.join(", ", duplicated)
                    + ". One would shadow the other, and which one wins would depend on bean ordering.");
        }

        Map<String, PreferenceDefinition> registered = new LinkedHashMap<>();

        declarations.stream()
                .sorted(Comparator.comparing(PreferenceDefinition::name))
                .forEach(declaration -> registered.put(declaration.name(), declaration));

        return new PreferenceCatalog(registered);
    }

    /** Every declaration, by name. */
    public List<PreferenceDefinition> declared() {
        return List.copyOf(declaredByName.values());
    }

    public Optional<PreferenceDefinition> find(String name) {
        return Optional.ofNullable(declaredByName.get(name));
    }

    /**
     * The declaration behind a name, or the refusal naming what could have been asked for.
     *
     * @throws AiPreferences.RefusedException always listing the declared names — a caller with a typo
     *                                        can otherwise only guess, and a caller with a stale name
     *                                        cannot tell the two apart
     */
    public PreferenceDefinition require(String name) {
        return find(name).orElseThrow(() -> new AiPreferences.RefusedException(
                "There is no preference called '" + name + "'. Declared: "
                + (declaredByName.isEmpty() ? "none" : String.join(", ", declaredByName.keySet())) + "."));
    }

    public boolean contains(String name) {
        return declaredByName.containsKey(name);
    }

    public int size() {
        return declaredByName.size();
    }
}
