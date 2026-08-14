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

    public String getId() {
        return id;
    }

    public String getApplication() {
        return application;
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
}
