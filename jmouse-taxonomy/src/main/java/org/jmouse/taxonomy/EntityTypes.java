package org.jmouse.taxonomy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 📚 The entity types a host registered: the only names an assignment may use.
 */
public final class EntityTypes {

    private final Map<String, EntityType> byName;

    private EntityTypes(Map<String, EntityType> byName) {
        this.byName = byName;
    }

    /**
     * 🏗️ The registry of these types.
     *
     * @throws TaxonomyException.Invalid when two share a name, or one has none
     */
    public static EntityTypes of(Collection<? extends EntityType> types) {
        Map<String, EntityType> byName = new LinkedHashMap<>();

        for (EntityType type : types) {
            if (type.name() == null || type.name().isBlank()) {
                throw new TaxonomyException.Invalid("An entity type is registered with a name: "
                        + type.getClass().getName() + " has none.");
            }
            if (byName.putIfAbsent(type.name(), type) != null) {
                throw new TaxonomyException.Invalid("Two entity types are registered as '" + type.name() + "'.");
            }
        }

        return new EntityTypes(Map.copyOf(byName));
    }

    public Optional<EntityType> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    /**
     * @throws TaxonomyException.Invalid naming the types there are, for a name none of them has
     */
    public EntityType require(String name) {
        return find(name).orElseThrow(() -> new TaxonomyException.Invalid("'" + name + "' is no entity type a category "
                + "may classify here; the registered ones are " + names() + "."));
    }

    public Set<String> names() {
        return byName.keySet();
    }
}
