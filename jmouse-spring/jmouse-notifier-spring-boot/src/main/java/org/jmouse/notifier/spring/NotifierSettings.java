package org.jmouse.notifier.spring;

import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Everything a product configures under {@code jmouse.notifier}.
 *
 * <pre>{@code
 * jmouse:
 *   notifier:
 *     store: memory                 # memory | database  — database needs jmouse-notifier-jpa
 *     capacity: 1000                # the memory store
 *     maximum-attempts: 5
 *     retry-interval: 10s           # how often the worker looks for due retries
 *     telegram:
 *       enabled: true               # when jmouse-notifier-telegram and a TelegramGateway are present
 *       purpose: notifications      # ⚠️ the identity purpose — point it at the SHARED notification bot
 *     screen:
 *       enabled: true               # when jmouse-notifier-web is present
 *       lifetime: 10m
 *       capacity: 500
 * }</pre>
 */
public record NotifierSettings(
        Store    store,
        int      capacity,
        int      maximumAttempts,
        Duration retryInterval,
        Telegram telegram,
        Screen   screen
) {

    public NotifierSettings {
        store           = store == null ? Store.MEMORY : store;
        capacity        = capacity <= 0 ? 1000 : capacity;
        maximumAttempts = maximumAttempts <= 0 ? 5 : maximumAttempts;
        retryInterval   = retryInterval == null || retryInterval.isNegative() || retryInterval.isZero()
                ? Duration.ofSeconds(10) : retryInterval;
        telegram        = telegram == null ? new Telegram(true, null) : telegram;
        screen          = screen == null ? new Screen(true, null, 0) : screen;
    }

    public static NotifierSettings defaults() {
        return new NotifierSettings(null, 0, 0, null, null, null);
    }

    public enum Store {
        MEMORY,
        DATABASE
    }

    /**
     * @param purpose the identity purpose the Telegram channel sends as. Default {@code notifications} —
     *                configure an identity under that purpose for the shared bot, or it falls back to
     *                {@code general}
     */
    public record Telegram(@DefaultValue("true") boolean enabled, String purpose) {

        public static final String DEFAULT_PURPOSE = "notifications";

        public Telegram {
            purpose = purpose == null || purpose.isBlank() ? DEFAULT_PURPOSE : purpose.strip();
        }
    }

    public record Screen(@DefaultValue("true") boolean enabled, Duration lifetime, int capacity) {

        public Screen {
            lifetime = lifetime == null || lifetime.isNegative() || lifetime.isZero() ? Duration.ofMinutes(10) : lifetime;
            capacity = capacity <= 0 ? 500 : capacity;
        }
    }
}
