package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.jdbc.bind.MissingParameterPolicy;
import org.jmouse.jdbc.core.*;

@BeanFactories
public class JdbcTemplateConfiguration {

    @Bean
    public CoreOperations coreOperations(JdbcExecutor executor) {
        return new CoreTemplate(executor);
    }

    @Bean
    public JdbcOperations jdbcOperations(CoreOperations core) {
        return new JdbcTemplate(core, MissingParameterPolicy.FAIL_FAST);
    }

}
