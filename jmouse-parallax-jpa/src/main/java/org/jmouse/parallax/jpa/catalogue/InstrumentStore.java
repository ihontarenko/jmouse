package org.jmouse.parallax.jpa.catalogue;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

/**
 * 🔭 The instruments: the visualizers and the labs, each a scene editor with its own starting point.
 *
 * <p>It does not authorize anything, and transaction demarcation is the caller's throughout.</p>
 */
public class InstrumentStore {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public InstrumentStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 📚 Every instrument, in the order they are shown.
     *
     * @return the instruments
     */
    public List<Instrument> allInOrder() {
        return entityManager.createQuery(
                        "select instrument from Instrument instrument order by instrument.position asc", Instrument.class)
                .getResultList();
    }

    /**
     * 🔎 One instrument.
     *
     * @param code its code
     * @return the instrument, or empty
     */
    public Optional<Instrument> find(String code) {
        return Optional.ofNullable(entityManager.find(Instrument.class, code));
    }

    /**
     * ❓ Whether an instrument exists.
     *
     * @param code its code
     * @return whether it does
     */
    public boolean exists(String code) {
        return find(code).isPresent();
    }

    /**
     * 💾 Keep an instrument: a new one is persisted, a loaded one is already tracked.
     *
     * @param instrument the instrument
     * @return the same instrument, now managed
     */
    public Instrument save(Instrument instrument) {
        if (!entityManager.contains(instrument)) {
            entityManager.persist(instrument);
        }

        return instrument;
    }
}
