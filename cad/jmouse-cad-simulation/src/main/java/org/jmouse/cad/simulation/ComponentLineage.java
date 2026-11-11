package org.jmouse.cad.simulation;

import org.jmouse.cad.component.Component;
import org.jmouse.cad.document.DocumentNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A component and the components it extends, from the most general to itself: {@code bipolar-transistor},
 * {@code npn-transistor}, {@code 2n2222}. A base the catalogue does not hold is refused, never read past — a part without
 * its base's physics is no part at all, and nothing else would say so.
 */
final class ComponentLineage {

    private ComponentLineage() {
    }

    static List<Component> of(Component component, CatalogueDocuments catalogue) {
        List<Component> lineage = new ArrayList<>();
        Set<String> path = new LinkedHashSet<>();
        Component current = component;
        while (true) {
            if (!path.add(current.head().slug())) {
                throw new IllegalArgumentException("'" + component.head().slug() + "' extends itself: "
                        + String.join(" -> ", path) + " -> " + current.head().slug());
            }
            lineage.addFirst(current);
            if (current.bases().isEmpty()) {
                return lineage;
            }
            if (current.bases().size() > 1) {
                throw new IllegalArgumentException(current.head().title() + " extends " + current.bases()
                        + ": a component extends one base");
            }
            current = base(current, current.bases().getFirst(), catalogue);
        }
    }

    private static Component base(Component member, String slug, CatalogueDocuments catalogue) {
        DocumentNode document = catalogue.find(slug).orElseThrow(() -> new IllegalArgumentException(member.head().title()
                + " extends '" + slug + "', which the catalogue does not hold."));
        if (!document.kind().equals(SimulationModule.COMPONENT)) {
            throw new IllegalArgumentException(member.head().title() + " extends '" + slug + "', which is a "
                    + document.kind().word() + ", not a component.");
        }
        return SimulationReading.component(document);
    }
}
