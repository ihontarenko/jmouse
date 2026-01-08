package org.jmouse.jdbc.configuration;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanLifecycleException;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.events.BeanContextEventPayload;
import org.jmouse.core.events.AbstractEventListener;
import org.jmouse.core.events.Event;
import org.jmouse.core.events.EventManager;
import org.jmouse.core.events.annotation.Listener;

import javax.sql.DataSource;

/**
 * Fail-fast guard for JDBC bootstrap: ensures that a {@link DataSource} bean exists.
 *
 * <p>Listens to {@code bean.lookup.not_found} events emitted by the bean container during
 * dependency resolution. If the missing required type is {@link DataSource}, this listener
 * throws a {@link BeanLifecycleException} with a clear configuration hint.</p>
 *
 * <p>Additionally, during initialization it may configure the {@link EventManager} error handler
 * to rethrow listener failures as {@link BeanLifecycleException}, ensuring event pipeline errors
 * abort container startup deterministically.</p>
 */
@Bean
@Listener(events = "bean.lookup.not_found")
public class MissingDataSourceConfiguration
        extends AbstractEventListener<BeanContextEventPayload.LookupPayload>
        implements InitializingBeanSupport<BeanContext> {

    private static final String MISSING_DATASOURCE_MESSAGE =
            "Missing DataSource configuration: no 'javax.sql.DataSource' bean is present. " +
                    "Define a DataSource bean to enable JDBC support.";

    @Override
    public void onEvent(Event<BeanContextEventPayload.LookupPayload> event) {
        if (requiresDataSource(event)) {
            throw new BeanLifecycleException(MISSING_DATASOURCE_MESSAGE);
        }
    }

    private static boolean requiresDataSource(Event<BeanContextEventPayload.LookupPayload> event) {
        Class<?> requiredType = event.payload().requiredType();

        if (requiredType == null) {
            return false;
        }

        return DataSource.class.isAssignableFrom(requiredType);
    }

    @Override
    public Class<?> payloadType() {
        return BeanContextEventPayload.LookupPayload.class;
    }

    @Override
    public void doInitialize(BeanContext context) {
        EventManager events = context.getEventManager();

        if (events == null) {
            return;
        }

        events.setErrorHandler((event, listener, error) -> {
            if (error instanceof BeanLifecycleException ble) {
                throw ble;
            }
            throw new BeanLifecycleException("Event listener failure: " + safeName(listener), error);
        });
    }

    private static String safeName(Object listener) {
        try {
            return String.valueOf(listener);
        } catch (Exception ignored) {
            return listener != null ? listener.getClass().getName() : "<null>";
        }
    }
}
