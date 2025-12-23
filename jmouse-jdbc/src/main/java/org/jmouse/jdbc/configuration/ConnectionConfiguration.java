package org.jmouse.jdbc.configuration;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.jdbc.connection.*;

import javax.sql.DataSource;

@BeanFactories
public class ConnectionConfiguration {

    public static final String DATA_SOURCE_CONNECTION_PROVIDER       = "dataSourceConnectionProvider";
    public static final String PRIMARY_CONNECTION_PROVIDER           = "primaryConnectionProvider";
    public static final String TRANSACTION_AWARE_CONNECTION_PROVIDER = "transactionAwareConnectionProvider";
    public static final String DRIVER_MANAGER_CONNECTION_PROVIDER    = "driverManagerConnectionProvider";
    public static final String CONNECTION_PROVIDER_SELECTOR          = "connectionProviderSelector";

    @PrimaryBean
    @Bean(PRIMARY_CONNECTION_PROVIDER)
    public ConnectionProvider primaryConnectionProvider(ConnectionProviderSelector selector) {
        return selector.select();
    }

    @Bean(DATA_SOURCE_CONNECTION_PROVIDER)
    public ConnectionProvider dataSourceConnectionProvider(DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

    @Bean(TRANSACTION_AWARE_CONNECTION_PROVIDER)
    public ConnectionProvider transactionAwareConnectionProvider(
            @Qualifier(DATA_SOURCE_CONNECTION_PROVIDER) ConnectionProvider rawConnectionProvider
    ) {
        return new TransactionAwareConnectionProvider(rawConnectionProvider);
    }

    @Bean(DRIVER_MANAGER_CONNECTION_PROVIDER)
    public ConnectionProvider driverManagerConnectionProvider(UserPasswordConnectionConfiguration configuration) {
        return DriverManagerConnectionProvider.of(
                configuration.url(), configuration.username(), configuration.password()
        );
    }

    @PrimaryBean
    @Bean(CONNECTION_PROVIDER_SELECTOR)
    public ConnectionProviderSelector connectionProviderSelector(BeanContext context) {
        return new ConnectionProviderSelector(context);
    }

}
