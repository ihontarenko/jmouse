package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.jdbc.connection.ConnectionProvider;
import org.jmouse.jdbc.connection.DataSourceConnectionProvider;
import org.jmouse.jdbc.connection.TransactionAwareConnectionProvider;
import org.jmouse.transaction.configuration.TransactionInfrastructureConfiguration;
import org.jmouse.transaction.infrastructure.TransactionContextHolder;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;

import javax.sql.DataSource;

@BeanFactories
public class JdbcConnectionProviderConfiguration {

    public static final String RAW_PROVIDER                = "rawConnectionProvider";
    public static final String PRIMARY_CONNECTION_PROVIDER = "connectionProvider";

    @Bean(RAW_PROVIDER)
    public ConnectionProvider dataSourceConnectionProvider(DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

    @PrimaryBean
    @Bean(PRIMARY_CONNECTION_PROVIDER)
    public ConnectionProvider primaryConnectionProvider(
            @Qualifier(RAW_PROVIDER) ConnectionProvider connectionProvider,
            @Qualifier(TransactionInfrastructureConfiguration.TX_CONTEXT_HOLDER) TransactionContextHolder transactionContextHolder
    ) {
        TransactionContextAccessSupport.register(transactionContextHolder);
        return new TransactionAwareConnectionProvider(connectionProvider);
    }
}

