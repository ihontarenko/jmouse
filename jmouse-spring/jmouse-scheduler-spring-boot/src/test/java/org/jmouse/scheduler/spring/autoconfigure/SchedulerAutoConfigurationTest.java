package org.jmouse.scheduler.spring.autoconfigure;

import org.h2.jdbcx.JdbcDataSource;
import org.jmouse.scheduler.GateRegistry;
import org.jmouse.scheduler.HandlerRegistry;
import org.jmouse.scheduler.InProcessDispatcher;
import org.jmouse.scheduler.ManualClock;
import org.jmouse.scheduler.Occurrence;
import org.jmouse.scheduler.Schedule;
import org.jmouse.scheduler.Scheduler;
import org.jmouse.scheduler.SchedulerRunner;
import org.jmouse.scheduler.Trigger;
import org.jmouse.scheduler.jdbc.JdbcTriggerStore;
import org.jmouse.scheduler.spi.GateVerdict;
import org.jmouse.scheduler.spi.TriggerDispatcher;
import org.jmouse.scheduler.spi.TriggerGate;
import org.jmouse.scheduler.spi.TriggerHandler;
import org.jmouse.scheduler.spi.TriggerStore;
import org.jmouse.scheduler.spring.SchedulerProperties;
import org.jmouse.scheduler.store.InMemoryTriggerStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerAutoConfigurationTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SchedulerAutoConfiguration.class));

    @Test
    void nothingIsWiredUnlessEnabled() {
        contextRunner
                .withUserConfiguration(Handlers.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(Scheduler.class);
                    assertThat(context).doesNotHaveBean(SchedulerRunner.class);
                    assertThat(context).doesNotHaveBean(TriggerStore.class);
                });
    }

    @Test
    void theJdbcStoreIsBuiltFromTheDataSourceAndHandlersAreCollectedByType() {
        contextRunner
                .withUserConfiguration(Database.class, Handlers.class)
                .withPropertyValues("jmouse.scheduler.enabled=true", "jmouse.scheduler.store.initialize-schema=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(TriggerStore.class).isInstanceOf(JdbcTriggerStore.class);
                    assertThat(context).hasSingleBean(SchedulerRunner.class);
                    assertThat(context).hasSingleBean(InProcessDispatcher.class);
                    assertThat(context.getBean(HandlerRegistry.class).keys()).containsExactly("scan-source", "vectorise-waiting");
                    assertThat(context.getBean(GateRegistry.class).keys()).containsExactly("budget-preflight");
                    assertThat(context.getBean(SchedulerRunner.class).running()).isTrue();

                    Scheduler scheduler = context.getBean(Scheduler.class);
                    scheduler.register(Trigger.builder("nightly-vectorise")
                            .handler("vectorise-waiting")
                            .schedule(Schedule.cron("0 3 * * *", java.time.ZoneId.of("Europe/Kyiv")))
                            .gate("budget-preflight")
                            .build());
                    assertThat(scheduler.triggers()).hasSize(1);
                });
    }

    @Test
    void theJdbcStoreWithoutADataSourceFailsTheStartUpNamingTheProperty() {
        contextRunner
                .withUserConfiguration(Handlers.class)
                .withPropertyValues("jmouse.scheduler.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause()
                            .hasMessageContaining("jmouse.scheduler.store.type");
                });
    }

    @Test
    void propertiesAreBound() {
        contextRunner
                .withUserConfiguration(Handlers.class)
                .withPropertyValues(
                        "jmouse.scheduler.enabled=true",
                        "jmouse.scheduler.poll-interval=2s",
                        "jmouse.scheduler.maximum-jitter=500ms",
                        "jmouse.scheduler.misfire-threshold=10m",
                        "jmouse.scheduler.batch-size=7",
                        "jmouse.scheduler.lease-duration=45s",
                        "jmouse.scheduler.store.type=memory",
                        "jmouse.scheduler.store.history-limit=12")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SchedulerProperties properties = context.getBean(SchedulerProperties.class);
                    assertThat(properties.settings().pollInterval()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(properties.settings().maximumJitter()).isEqualTo(Duration.ofMillis(500));
                    assertThat(properties.settings().misfireThreshold()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(properties.settings().batchSize()).isEqualTo(7);
                    assertThat(properties.settings().leaseDuration()).isEqualTo(Duration.ofSeconds(45));
                    assertThat(properties.store().historyLimit()).isEqualTo(12);
                    assertThat(context).getBean(TriggerStore.class).isInstanceOf(InMemoryTriggerStore.class);
                });
    }

    @Test
    void aBadSettingsCombinationFailsTheStartUp() {
        contextRunner
                .withUserConfiguration(Handlers.class)
                .withPropertyValues("jmouse.scheduler.enabled=true", "jmouse.scheduler.store.type=memory",
                        "jmouse.scheduler.misfire-threshold=1s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void aProductDispatcherReplacesTheInProcessOne() {
        contextRunner
                .withUserConfiguration(Handlers.class, QueueDispatcher.class)
                .withPropertyValues("jmouse.scheduler.enabled=true", "jmouse.scheduler.store.type=memory")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(InProcessDispatcher.class);
                    assertThat(context).hasSingleBean(TriggerDispatcher.class);
                });
    }

    @Test
    void pollingOffLeavesTheSchedulerButNoLoop() {
        contextRunner
                .withUserConfiguration(Handlers.class)
                .withPropertyValues("jmouse.scheduler.enabled=true", "jmouse.scheduler.store.type=memory",
                        "jmouse.scheduler.polling=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Scheduler.class);
                    assertThat(context).doesNotHaveBean(SchedulerRunner.class);
                });
    }

    @Test
    void theApplicationClockDrivesTheSchedulerAndHandlersRunInProcess() {
        contextRunner
                .withUserConfiguration(Handlers.class, TestClock.class)
                .withPropertyValues("jmouse.scheduler.enabled=true", "jmouse.scheduler.store.type=memory",
                        "jmouse.scheduler.poll-interval=1h", "jmouse.scheduler.maximum-jitter=0s",
                        "jmouse.scheduler.misfire-threshold=2h")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ManualClock clock = context.getBean(ManualClock.class);
                    context.getBean(Scheduler.class).register(Trigger.builder("source-1.scan")
                            .handler("scan-source")
                            .schedule(Schedule.every(Duration.ofMinutes(5)))
                            .build());
                    clock.advance(Duration.ofMinutes(5));

                    assertThat(context.getBean(SchedulerRunner.class).pollOnce()).isEqualTo(1);
                    assertThat(Handlers.SCANNED.await(5, TimeUnit.SECONDS)).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class Handlers {

        static final CountDownLatch SCANNED = new CountDownLatch(1);

        @Bean
        TriggerHandler scanSource() {
            return new TriggerHandler() {
                @Override
                public String key() {
                    return "scan-source";
                }

                @Override
                public void handle(Occurrence occurrence) {
                    SCANNED.countDown();
                }
            };
        }

        @Bean
        TriggerHandler vectoriseWaiting() {
            return new TriggerHandler() {
                @Override
                public String key() {
                    return "vectorise-waiting";
                }

                @Override
                public void handle(Occurrence occurrence) {
                }
            };
        }

        @Bean
        TriggerGate budgetPreflight() {
            return new TriggerGate() {
                @Override
                public String key() {
                    return "budget-preflight";
                }

                @Override
                public GateVerdict evaluate(Occurrence occurrence) {
                    return GateVerdict.pass();
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Database {

        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:boot-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
            return dataSource;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class QueueDispatcher {

        @Bean
        TriggerDispatcher backgroundJobsDispatcher() {
            return occurrence -> {
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestClock {

        @Bean
        ManualClock clock() {
            return new ManualClock(START);
        }
    }
}
