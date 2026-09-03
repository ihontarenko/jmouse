package org.jmouse.money.spring.autoconfigure;

import org.jmouse.money.spring.ExchangeRateService;
import org.jmouse.money.spring.MoneyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

/**
 * 🕒 The automatic sync — which does not exist unless somebody asks for it.
 *
 * <h3>⚠️ Off by default, and that is a decision rather than caution</h3>
 *
 * <p>{@code jmouse.money.sync-cron} is empty out of the box, and an empty value registers nothing at
 * all. A library that begins making requests to a national bank because it landed on a classpath is a
 * library that surprises somebody — possibly the bank.</p>
 *
 * <h3>⚠️ A {@link SchedulingConfigurer}, not {@code @Scheduled}</h3>
 *
 * <p>{@code @Scheduled(cron = "${…}")} is parsed whether or not the value makes sense, so an
 * application that set the property to an empty string would fail to start with a cron parse error
 * rather than simply not scheduling. Registering the task by hand lets "no cron" mean no task.</p>
 *
 * <p>An application that never enabled scheduling ignores this bean entirely, which is the right
 * outcome: no task, no error, no surprise.</p>
 */
@AutoConfiguration
@AutoConfigureAfter(MoneyAutoConfiguration.class)
@ConditionalOnClass(SchedulingConfigurer.class)
@ConditionalOnBean(ExchangeRateService.class)
public class MoneyScheduledSyncAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoneyScheduledSyncAutoConfiguration.class);

    @Bean
    public SchedulingConfigurer moneySyncScheduler(ExchangeRateService service, MoneyProperties properties) {
        return registrar -> register(registrar, service, properties.getSyncCron());
    }

    private static void register(ScheduledTaskRegistrar registrar, ExchangeRateService service, String cron) {
        if (cron == null || cron.isBlank()) {
            LOGGER.debug("💱 No jmouse.money.sync-cron set — rates will only move when somebody asks");
            return;
        }

        registrar.addTriggerTask(() -> syncQuietly(service), new CronTrigger(cron));

        LOGGER.info("💱 Exchange rates will sync on '{}'", cron);
    }

    /**
     * ⚠️ A scheduled task that throws is a task that stops being scheduled in some setups, and a bank
     * being briefly unreachable is an ordinary night rather than a reason to stop syncing for ever.
     * Logged and swallowed; the rates simply keep their previous values, which the age shown beside any
     * converted total already discloses.
     */
    private static void syncQuietly(ExchangeRateService service) {
        try {
            ExchangeRateService.SyncOutcome outcome = service.sync();

            LOGGER.info("💱 Scheduled sync from '{}': {} written, {} left as manual",
                        outcome.provider(), outcome.written(), outcome.leftAsManual());
        } catch (RuntimeException failure) {
            LOGGER.warn("💱 Scheduled rate sync failed; the stored rates are unchanged", failure);
        }
    }
}
