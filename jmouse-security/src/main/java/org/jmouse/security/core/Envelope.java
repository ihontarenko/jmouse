package org.jmouse.security.core;

import java.util.Map;

/**
 * ✉️ Security envelope: carries all data for an authorization decision.
 *
 * <p>Includes subject, operation, resource, credentials and
 * mutable {@link Attributes}.</p>
 */
public interface Envelope extends Attributes {

    /**
     * 🙋 The acting subject (user, service, etc.).
     */
    Subject subject();

    /**
     * 🎬 The requested operation.
     */
    Operation operation();

    /**
     * 📦 The target resource.
     */
    Resource resource();

    /**
     * 🧾 Attribute bag (contextual key-value pairs).
     */
    Attributes attributes();

    /**
     * 🔑 Credential carrier (tokens, certs, secrets).
     */
    CredentialCarrier carrier();

    /**
     * ➕ Return a new envelope with replaced attributes.
     */
    Envelope with(Attributes attributes);

    /**
     * 👤 Return a new envelope with replaced subject.
     */
    Envelope with(Subject subject);

    // ---- Attribute delegation ----

    @Override
    default Object get(Object key) {
        return attributes().get(key);
    }

    @Override
    default void set(Object key, Object value) {
        attributes().set(key, value);
    }

    @Override
    default void batch(Map<?, Object> values) {
        attributes().batch(values);
    }

    @Override
    default void remove(Object key) {
        attributes().remove(key);
    }

    @Override
    default void clear() {
        attributes().clear();
    }

    @Override
    default Map<Object, Object> asMap() {
        return attributes().asMap();
    }
}
