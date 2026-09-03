package org.jmouse.money.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ⚙️ What an application says about money.
 *
 * <p>⚠️ <strong>The pivot here is NOT a product's base currency.</strong> It is what the rate table is
 * quoted against, which is a property of the feed — the National Bank of Ukraine publishes hryvnia, and
 * so the pivot is the hryvnia whatever the product counts in. A product that totals in dollars converts
 * through this pivot and never has to know it exists. Setting it to match a product's display currency
 * is the mistake this paragraph is here to prevent: it would silently ask the feed for something it does
 * not publish.</p>
 */
@ConfigurationProperties(prefix = "jmouse.money")
public class MoneyProperties {

    /**
     * 💱 The currency the stored rates are quoted against. Defaults to the NBU's.
     *
     * <p>⚠️ Changing it on an installation that already has rows leaves the table holding two pivots,
     * which every row individually survives and the set of them does not. The sync refuses rather than
     * mixing them — see {@code ExchangeRateService}.</p>
     */
    private String pivot = "UAH";

    /** 📡 Which feed to ask. Only {@code nbu} ships with the library. */
    private String provider = "nbu";

    /** 🌐 Where that feed lives — overridable so a test or a mirror can stand in for the bank. */
    private String baseUrl = "https://bank.gov.ua";

    /**
     * 🕒 Cron expression for the automatic sync. ⚠️ <strong>Empty means no automatic sync at all</strong>,
     * and that is the default on purpose: a library that starts calling a foreign bank because it landed
     * on somebody's classpath is a library that surprises people.
     */
    private String syncCron = "";

    /** 🚚 Whether this library migrates its own schema. Off only for a product that runs the SQL itself. */
    private boolean migrationsEnabled = true;

    public String getPivot() {
        return pivot;
    }

    public void setPivot(String pivot) {
        this.pivot = pivot;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSyncCron() {
        return syncCron;
    }

    public void setSyncCron(String syncCron) {
        this.syncCron = syncCron;
    }

    public boolean isMigrationsEnabled() {
        return migrationsEnabled;
    }

    public void setMigrationsEnabled(boolean migrationsEnabled) {
        this.migrationsEnabled = migrationsEnabled;
    }
}
