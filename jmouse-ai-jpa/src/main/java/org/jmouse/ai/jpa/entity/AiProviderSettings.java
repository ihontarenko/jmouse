package org.jmouse.ai.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Which model an application talks to, on whose key, and whether this row is the one in force.
 *
 * <p>A row rather than a property file because <strong>rotating a key or switching provider must not
 * need a restart</strong>. That is the whole reason the settings are read per call: the moment somebody
 * needs to change a key is the moment they least want to redeploy.
 *
 * <p>⚠️ <strong>{@link #application} is the lookup key, not part of a call.</strong> A
 * {@code ChatRequest} carries no such field, deliberately — conflating the two is what stops a gateway
 * being usable as a library. Several rows may exist for one application; exactly one of them should be
 * {@link #active}, and the source refuses rather than guesses when none is.
 *
 * <p>⚠️ The key is stored as given. Encrypting it at rest is a product's decision and belongs behind
 * whatever secret handling that product already has — a library that encrypted it would be choosing
 * the key management too.
 */
@Entity
@Table(name = AiProviderSettings.TABLE_NAME)
public class AiProviderSettings {

    public static final String TABLE_NAME = "ai_provider_settings";

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "application", length = 64, nullable = false)
    private String application;

    /**
     * What this configuration is FOR, in the product's own words.
     *
     * <h2>⚠️ NULLABLE, and that is the migration rather than an oversight</h2>
     *
     * <p>Every row written before purposes existed has none, and a row with none answers <em>every</em>
     * purpose — see {@code JpaProviderSettingsSource}. So an installation that configured one provider
     * years ago keeps working untouched, and starts being specific only when somebody chooses to be.
     *
     * <p>⚠️ Free text, keyed by the product. This module has no idea what "movie matching" is and must
     * not learn: an enum here would mean a release of this library for every purpose any product
     * invents.
     */
    @Column(name = "purpose", length = 64)
    private String purpose;

    /** Matched against {@code ChatModel.providerName()}, which refuses settings addressed elsewhere. */
    @Column(name = "provider", length = 32, nullable = false)
    private String provider;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    /** Null or blank means the provider's own address. */
    @Column(name = "api_url", length = 255)
    private String apiUrl;

    @Column(name = "model", length = 128, nullable = false)
    private String model;

    @Column(name = "max_tokens", nullable = false)
    private int maximumTokens;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AiProviderSettings() {
    }

    /**
     * A new row, as an administration screen creates one.
     *
     * <p>⚠️ <strong>Writable on purpose, and the reason is the class's own first paragraph.</strong>
     * The point of a row rather than a property file is that a key can be rotated without a restart —
     * and that is only true if something can write it. An entity with no constructor and no mutators
     * described a table nothing could administer, which made the promise at the top of this file
     * unkeepable. What may be changed is deliberately narrower than the field list: an identifier and
     * a creation time are facts about the row rather than settings, and neither has a mutator.
     */
    public AiProviderSettings(
            String        id,
            String        application,
            String        provider,
            String        apiKey,
            String        apiUrl,
            String        model,
            int           maximumTokens,
            boolean       active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        this.id            = id;
        this.application   = application;
        this.provider      = provider;
        this.apiKey        = apiKey;
        this.apiUrl        = apiUrl;
        this.model         = model;
        this.maximumTokens = maximumTokens;
        this.active        = active;
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getApplication() {
        return application;
    }

    /** ⚠️ May be null — a row written before purposes existed, which answers every purpose. */
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose == null || purpose.isBlank() ? null : purpose.trim();
    }

    public String getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModel() {
        return model;
    }

    public int getMaximumTokens() {
        return maximumTokens;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ── What an administration screen may change ─────────────────────────────────
    //
    // Everything a person configures, and nothing else. The identifier and the creation time are
    // facts about the row rather than settings, and the application is what the row is *about* —
    // moving a configuration to another application is deleting one and creating another, which is
    // the honest way to describe what happens to whatever was already talking through it.

    public void setProvider(String provider) {
        this.provider = provider;
    }

    /** ⚠️ Only when a new one was actually given: a blank field on a form must not erase the key. */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setMaximumTokens(int maximumTokens) {
        this.maximumTokens = maximumTokens;
    }

    /**
     * ⚠️ Exactly one row per application should carry this, and nothing here enforces it — the
     * constraint spans rows, so it belongs to whatever administers them.
     * {@link org.jmouse.ai.jpa.JpaProviderSettingsSource} refuses rather than guesses when more than
     * one is active, which is the loud half of the same rule.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
