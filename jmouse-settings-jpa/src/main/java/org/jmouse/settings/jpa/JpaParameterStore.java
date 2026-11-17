package org.jmouse.settings.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.settings.ParameterStore;
import org.jmouse.settings.SettingHolder;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 🎚️ The settings' values as rows of {@code settings_parameters}. It checks nothing — a value is checked against its section before
 * it reaches here — and transaction demarcation is the caller's throughout.
 */
public class JpaParameterStore implements ParameterStore {

    private final EntityManager entityManager;
    private final Clock         clock;

    public JpaParameterStore(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public Map<String, String> kept(SettingHolder holder, String owner, String section) {
        Map<String, String> values = new LinkedHashMap<>();
        rows(holder, owner, section).forEach(row -> values.put(row.getKey().getParameter(), row.getValue()));
        return values;
    }

    @Override
    public void keep(SettingHolder holder, String owner, String section, Map<String, String> values) {
        values.forEach((parameter, value) -> {
            KeptParameter.Key key = new KeptParameter.Key(holder.type(), holder.id(), owner, section, parameter);
            KeptParameter row = entityManager.find(KeptParameter.class, key);
            if (row == null) {
                entityManager.persist(new KeptParameter(key, value, clock.instant()));
            } else if (!Objects.equals(row.getValue(), value)) {
                row.change(value, clock.instant());
            }
        });
    }

    @Override
    public void forget(SettingHolder holder, String owner, String section, Set<String> keys) {
        keys.forEach(parameter -> {
            KeptParameter row = entityManager.find(KeptParameter.class, new KeptParameter.Key(holder.type(), holder.id(), owner, section, parameter));
            if (row != null) {
                entityManager.remove(row);
            }
        });
    }

    @Override
    public void replace(SettingHolder holder, String owner, String section, Map<String, String> values) {
        for (KeptParameter row : rows(holder, owner, section)) {
            if (!values.containsKey(row.getKey().getParameter())) {
                entityManager.remove(row);
            }
        }
        // The removals reach the table before a row of the same key could be written again.
        entityManager.flush();
        keep(holder, owner, section, values);
    }

    @Override
    public Map<String, Map<String, String>> keptUnder(SettingHolder holder, String owner, String section) {
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        entityManager.createQuery("select row from KeptParameter row where row.key.holderType = :holderType and row.key.holderId = :holderId "
                                  + "and row.key.owner = :owner and (row.key.section = :section or row.key.section like :below) "
                                  + "order by row.key.section, row.key.parameter", KeptParameter.class)
                .setParameter("holderType", holder.type())
                .setParameter("holderId", holder.id())
                .setParameter("owner", owner)
                .setParameter("section", section)
                .setParameter("below", section + "/%")
                .getResultList()
                .forEach(row -> sections.computeIfAbsent(row.getKey().getSection(), unknown -> new LinkedHashMap<>())
                        .put(row.getKey().getParameter(), row.getValue()));
        return sections;
    }

    private List<KeptParameter> rows(SettingHolder holder, String owner, String section) {
        return entityManager.createQuery("select row from KeptParameter row where row.key.holderType = :holderType "
                                         + "and row.key.holderId = :holderId and row.key.owner = :owner and row.key.section = :section "
                                         + "order by row.key.parameter",
                                         KeptParameter.class)
                .setParameter("holderType", holder.type())
                .setParameter("holderId", holder.id())
                .setParameter("owner", owner)
                .setParameter("section", section)
                .getResultList();
    }
}
