package org.jmouse.beans.events;

/**
 * 🧭 Defines all lifecycle and operational events emitted by a {@code BeanContext}.
 * <p>
 * Event names represent distinct stages of context startup,
 * bean definition registration, bean lookup, creation,
 * initialization, and error handling.
 *
 * <h3>Usage</h3>
 * Used together with {@link BeanContextEventPayload} to provide
 * structured and observable bean context behavior.
 */
public enum BeanContextEventName {

    /**
     * 🚀 Emitted before context refresh begins.
     */
    CONTEXT_REFRESH_START,

    /**
     * ✅ Emitted after context refresh successfully completes.
     */
    CONTEXT_REFRESH_FINISH,

    /**
     * 📘 Emitted when a bean definition is successfully registered.
     */
    DEFINITION_REGISTERED,

    /**
     * ⚠️ Emitted when a duplicate bean definition is detected.
     */
    DEFINITION_DUPLICATE,

    /**
     * 🔍 Emitted when bean lookup starts.
     */
    BEAN_LOOKUP_START,

    /**
     * 🎯 Emitted when a bean is successfully resolved.
     */
    BEAN_FOUND,

    /**
     * ❓ Emitted when no matching bean is found.
     */
    BEAN_NOT_FOUND,

    /**
     * ⚠️ Emitted when lookup by type finds multiple candidates
     * and no primary bean can be selected.
     */
    BEAN_LOOKUP_AMBIGUOUS,

    /**
     * ⭐ Emitted when a primary bean is selected
     * among multiple matching candidates.
     */
    BEAN_PRIMARY_SELECTED,

    /**
     * 🏗 Emitted before bean instantiation begins.
     */
    BEAN_CREATE_START,

    /**
     * ✅ Emitted after a bean instance is successfully created.
     */
    BEAN_CREATED,

    /**
     * ❌ Emitted when bean creation fails.
     */
    BEAN_CREATE_FAILED,

    /**
     * ⚙️ Emitted before bean initialization begins.
     */
    BEAN_INIT_START,

    /**
     * 🔁 Emitted before custom initialization callbacks are invoked.
     */
    BEAN_PROCESSED_BEFORE_INIT,

    /**
     * 🧩 Emitted when a {@code BeanInitializer} is invoked.
     */
    BEAN_INITIALIZER_INVOKED,

    /**
     * 🔁 Emitted after custom initialization callbacks are completed.
     */
    BEAN_PROCESSED_AFTER_INIT,

    /**
     * 🏁 Emitted after bean initialization fully completes.
     */
    BEAN_INIT_FINISH,

    /**
     * 🚨 Emitted when a context-level error occurs.
     */
    CONTEXT_ERROR,

    /**
     * 🚨 Emitted for unrecoverable or uncategorized errors.
     */
    GENERAL_ERROR
}
