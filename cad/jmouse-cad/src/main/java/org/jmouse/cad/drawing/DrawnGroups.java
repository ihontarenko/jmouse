package org.jmouse.cad.drawing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The groups a drawing declares, asked whether an item is held where it stands: locked itself, or in a locked group. A group an
 * item names that the drawing does not declare is a free one — a member's name alone makes it.
 */
public final class DrawnGroups {

    private final Map<String, DrawnGroup> groups = new LinkedHashMap<>();

    public DrawnGroups(List<DrawnGroup> declared) {
        for (DrawnGroup group : declared) {
            groups.put(group.name(), group);
        }
    }

    /** The group of a name, when the drawing declares it. */
    public Optional<DrawnGroup> named(String name) {
        return Optional.ofNullable(groups.get(name));
    }

    /** Whether an item is held where it stands: locked itself, or a member of a locked group. */
    public boolean holds(Arrangement arrangement) {
        return arrangement.locked() || arrangement.grouped() && named(arrangement.group()).map(DrawnGroup::locked).orElse(false);
    }

    /** What holds an item, said for a refusal: the item itself, or its group by name; empty when it is free. */
    public Optional<String> holder(Arrangement arrangement) {
        if (arrangement.locked()) {
            return Optional.of("it is locked");
        }
        if (holds(arrangement)) {
            return Optional.of("its group '" + arrangement.group() + "' is locked");
        }
        return Optional.empty();
    }
}
