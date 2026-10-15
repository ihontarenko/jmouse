/**
 * Durable triggers, fired by a single polling loop and claimed across processes.
 *
 * <p>A {@link org.jmouse.scheduler.Trigger} is data: a {@link org.jmouse.scheduler.Schedule}
 * ({@code Cron}, {@code Interval} or {@code Once}), a handler key with
 * {@link org.jmouse.scheduler.TriggerParameters}, a {@link org.jmouse.scheduler.MisfirePolicy} and an
 * optional gate key. {@link org.jmouse.scheduler.Scheduler} registers and reads triggers;
 * {@link org.jmouse.scheduler.SchedulerRunner} fires them.
 *
 * <p>The library dispatches and nothing more: what a fired
 * {@link org.jmouse.scheduler.Occurrence} turns into is the product's
 * {@link org.jmouse.scheduler.spi.TriggerDispatcher}. The seams are in
 * {@link org.jmouse.scheduler.spi}; the stores in {@link org.jmouse.scheduler.store} and
 * {@link org.jmouse.scheduler.jdbc}.
 *
 * <p>No Spring. The Spring Boot wiring is {@code jmouse-scheduler-spring-boot}.
 */
package org.jmouse.scheduler;
