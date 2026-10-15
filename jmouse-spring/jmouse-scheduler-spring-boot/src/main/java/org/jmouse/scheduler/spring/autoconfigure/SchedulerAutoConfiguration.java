package org.jmouse.scheduler.spring.autoconfigure;

import org.jmouse.scheduler.GateRegistry;
import org.jmouse.scheduler.HandlerRegistry;
import org.jmouse.scheduler.InProcessDispatcher;
import org.jmouse.scheduler.Scheduler;
import org.jmouse.scheduler.SchedulerRunner;
import org.jmouse.scheduler.jdbc.JdbcTriggerSchema;
import org.jmouse.scheduler.jdbc.JdbcTriggerStore;
import org.jmouse.scheduler.spi.TriggerDispatcher;
import org.jmouse.scheduler.spi.TriggerGate;
import org.jmouse.scheduler.spi.TriggerHandler;
import org.jmouse.scheduler.spi.TriggerStore;
import org.jmouse.scheduler.spring.SchedulerProperties;
import org.jmouse.scheduler.store.InMemoryTriggerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * ⚙️ The scheduler for a Spring Boot application: set {@code jmouse.scheduler.enabled=true}, declare
 * {@link TriggerHandler} beans, register triggers through the {@link Scheduler} bean.
 *
 * <h2>Rules every bean here follows</h2>
 *
 * <p><strong>Off unless enabled.</strong> Nothing is wired — no store, no thread — until
 * {@code jmouse.scheduler.enabled} is {@code true}. A library that starts a polling thread because a
 * jar is on the classpath is a library that fires somebody's triggers by surprise.
 *
 * <p><strong>Every bean steps aside.</strong> All are {@code @ConditionalOnMissingBean}: a product
 * dispatching into its own job queue declares a {@link TriggerDispatcher} bean and this configuration
 * goes quiet about that one thing only.
 *
 * <p><strong>The store is never chosen silently.</strong> {@code store.type=jdbc} (the default) with no
 * {@link DataSource} fails the start-up naming the property, rather than falling back to memory and
 * losing every trigger at the next restart.
 *
 * <h2>⚠️ When this does not run, the symptom is silence</h2>
 *
 * <p>The runner logs one line when it starts — {@code jmouse-scheduler started: store=…, handlers=[…]}.
 * If that line is absent with {@code enabled=true}, the auto-configuration did not register; establish
 * that before reading any Java.
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@ConditionalOnClass(Scheduler.class)
@ConditionalOnProperty(prefix = "jmouse.scheduler", name = "enabled", havingValue = "true")
public class SchedulerAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerAutoConfiguration.class);

    /** ⚙️ Settings bound out of the environment under {@code jmouse.scheduler}. */
    @Bean
    @ConditionalOnMissingBean
    public SchedulerProperties schedulerProperties(Environment environment) {
        return Binder.get(environment)
                .bind("jmouse.scheduler", SchedulerProperties.class)
                .orElseGet(SchedulerProperties::defaults);
    }

    /** ⚙️ Every {@link TriggerHandler} bean, by key. Two handlers on one key fail the start-up. */
    @Bean
    @ConditionalOnMissingBean
    public HandlerRegistry schedulerHandlerRegistry(ObjectProvider<TriggerHandler> handlers) {
        return new HandlerRegistry(handlers.orderedStream().toList());
    }

    /** ⚙️ Every {@link TriggerGate} bean, by key. */
    @Bean
    @ConditionalOnMissingBean
    public GateRegistry schedulerGateRegistry(ObjectProvider<TriggerGate> gates) {
        return new GateRegistry(gates.orderedStream().toList());
    }

    /**
     * ⚙️ The store: {@link JdbcTriggerStore} over the application's {@link DataSource}, or
     * {@link InMemoryTriggerStore} when {@code store.type=memory} says so explicitly.
     */
    @Bean
    @ConditionalOnMissingBean
    public TriggerStore schedulerTriggerStore(SchedulerProperties properties, ObjectProvider<DataSource> dataSources,
                                              ObjectProvider<Clock> clocks) {
        SchedulerProperties.Store store = properties.store();

        return switch (store.type()) {
            case MEMORY -> {
                LOGGER.warn("jmouse-scheduler: the in-memory trigger store is lost on restart and is not shared "
                        + "between processes; set jmouse.scheduler.store.type=jdbc for anything but a test");
                yield new InMemoryTriggerStore(store.historyLimit(), resolveClock(clocks));
            }
            case JDBC -> {
                DataSource dataSource = dataSources.getIfUnique();

                if (dataSource == null) {
                    throw new IllegalStateException("jmouse.scheduler.store.type=jdbc needs a single DataSource bean, "
                            + "and there is none; configure one, or set jmouse.scheduler.store.type=memory for a test");
                }

                if (store.initializeSchema()) {
                    JdbcTriggerSchema.create(dataSource);
                }

                yield new JdbcTriggerStore(dataSource, store.historyLimit(), resolveClock(clocks));
            }
        };
    }

    /**
     * ⚙️ Runs handlers in this process. A product with a durable job queue replaces this with a
     * dispatcher that enqueues, and runs the handlers from its own worker.
     */
    @Bean
    @ConditionalOnMissingBean(TriggerDispatcher.class)
    public InProcessDispatcher schedulerDispatcher(HandlerRegistry handlers) {
        return new InProcessDispatcher(handlers);
    }

    /** ⚙️ Registering and reading triggers. */
    @Bean
    @ConditionalOnMissingBean
    public Scheduler scheduler(TriggerStore store, HandlerRegistry handlers, GateRegistry gates,
                               ObjectProvider<Clock> clocks) {
        return new Scheduler(store, handlers, gates, resolveClock(clocks));
    }

    /**
     * ⚙️ The firing loop, started with the context and stopped with it ({@code AutoCloseable}).
     * Absent when {@code jmouse.scheduler.polling=false}, for a process that only administers triggers.
     */
    @Bean(initMethod = "start")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jmouse.scheduler", name = "polling", havingValue = "true", matchIfMissing = true)
    public SchedulerRunner schedulerRunner(TriggerStore store, HandlerRegistry handlers, GateRegistry gates,
                                           TriggerDispatcher dispatcher, SchedulerProperties properties,
                                           ObjectProvider<Clock> clocks) {
        return new SchedulerRunner(store, handlers, gates, dispatcher, resolveClock(clocks), properties.settings());
    }

    /** The application's own {@link Clock} when it has exactly one — which is how a test moves time. */
    private static Clock resolveClock(ObjectProvider<Clock> clocks) {
        Clock clock = clocks.getIfUnique();
        return clock == null ? Clock.systemUTC() : clock;
    }
}
