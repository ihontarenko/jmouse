package org.jmouse.telegram.spring.autoconfigure;

import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.RoutingGateway;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.bot.BotApiTransport;
import org.jmouse.telegram.pace.Pace;
import org.jmouse.telegram.pace.PacedTransport;
import org.jmouse.telegram.pace.TokenBucketPace;
import org.jmouse.telegram.spi.TelegramTransport;
import org.jmouse.telegram.spring.PropertiesIdentitySource;
import org.jmouse.telegram.spring.TelegramSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.jmouse.telegram.update.UpdateDispatcher;

import java.util.List;

/**
 * ⚙️ Everything a Spring Boot application needs to talk to Telegram: add the dependency, set a token,
 * inject a {@link TelegramGateway}.
 *
 * <h2>Two rules every bean here follows</h2>
 *
 * <p><strong>Every bean steps aside.</strong> All of them are conditional on the application not
 * declaring one of the same type, so a product with its own identity store — which is what `JMF-334`
 * builds — declares a bean and this configuration goes quiet about that one thing only.
 *
 * <p><strong>Nothing is started that was not asked for.</strong> No polling thread and no listener
 * exist unless {@code jmouse.telegram.updates.mode} says so; the default is {@code none}, which is
 * correct for the common case of an application that only sends.
 *
 * <h2>⚠️ When this does not run, the symptom is silence</h2>
 *
 * <p>Boot 4 split autoconfiguration into per-technology modules, and a jar on the classpath no longer
 * brings its Spring integration along for free. The mechanism is unchanged — this class is listed in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} — but a
 * failure to register produces no error at all. Hence {@link TelegramDiagnostics}, which logs what was
 * actually built: if that line is absent from the startup log, this did not run.
 */
@AutoConfiguration
@ConditionalOnClass(TelegramGateway.class)
public class TelegramAutoConfiguration {

    /**
     * ⚙️ Settings bound out of the environment under {@code jmouse.telegram}.
     *
     * <p>Bound rather than {@code @ConfigurationProperties}-scanned, so the record stays a plain value
     * type that a smoke class or a product's own code can construct without a Spring context.
     */
    @Bean
    @ConditionalOnMissingBean
    public TelegramSettings telegramSettings(Environment environment) {
        return Binder.get(environment)
                .bind("jmouse.telegram", TelegramSettings.class)
                .orElseGet(() -> new TelegramSettings(null, null, null, null));
    }

    /** ⚙️ Identities from properties. Replaced wholesale by a product with a store of its own. */
    @Bean
    @ConditionalOnMissingBean
    public IdentitySource telegramIdentitySource(TelegramSettings settings) {
        return new PropertiesIdentitySource(settings);
    }

    /**
     * ⚙️ The Bot API transport.
     *
     * <p>⚠️ Registered as a bean of its own concrete type as well as a {@link TelegramTransport},
     * because the polling loop needs it specifically — {@code getUpdates} is not on the shared SPI.
     */
    @Bean
    @ConditionalOnMissingBean
    public BotApiTransport botApiTransport() {
        return new BotApiTransport();
    }

    /**
     * ⚙️ How fast this application may talk to Telegram.
     *
     * <p>⚠️ On by default, and that is deliberate: the failure it prevents is the bot being
     * rate-limited, which delays <em>every</em> message rather than the one that was too eager. An
     * installation that runs more than one instance should replace this with a {@link Pace} over a
     * shared cache — the buckets here are per process, while Telegram counts the total.
     */
    @Bean
    @ConditionalOnMissingBean
    public Pace telegramPace(TelegramSettings settings) {
        TelegramSettings.Pacing pacing = settings.pace();

        if (!pacing.enabled()) {
            return Pace.unlimited();
        }

        return new TokenBucketPace(
                pacing.globalPerSecond(),
                pacing.perChatPerMinute(),
                TokenBucketPace.DEFAULT_TRACKED_CHATS);
    }

    /**
     * ⚙️ The gateway, over every transport the application has.
     *
     * <p>Each transport is wrapped in a {@link PacedTransport} here rather than being registered as one
     * — ⚠️ a paced wrapper published as a bean would be a <em>second</em> {@link TelegramTransport} of
     * the same kind, and {@link RoutingGateway} refuses that (correctly: two transports for one
     * protocol would otherwise resolve to whichever was last in the list).
     *
     * <p>A product adding a transport of its own declares it as a bean and it is picked up here,
     * paced like the rest.
     */
    @Bean
    @ConditionalOnMissingBean
    public TelegramGateway telegramGateway(
            IdentitySource identities, List<TelegramTransport> transports, Pace pace) {

        List<TelegramTransport> paced = transports.stream()
                .map(transport -> (TelegramTransport) new PacedTransport(transport, pace))
                .toList();

        return new RoutingGateway(identities, paced);
    }

    /**
     * ⚙️ Where updates go.
     *
     * <p>Always present, even when no ingestion runs: a product registers its handlers at startup and
     * should not have to know whether the polling loop or the webhook is the one configured. ⚠️ It is
     * also the fan-out that makes two products able to share one bot — Telegram delivers a bot's
     * updates to exactly one consumer.
     */
    @Bean
    @ConditionalOnMissingBean
    public UpdateDispatcher telegramUpdateDispatcher() {
        return new UpdateDispatcher();
    }

    /** ⚙️ One line at startup saying what exists, because the failure mode above is silence. */
    @Bean
    @ConditionalOnMissingBean
    public TelegramDiagnostics telegramDiagnostics(
            TelegramSettings settings, List<TelegramTransport> transports) {

        return new TelegramDiagnostics(settings, transports);
    }
}
