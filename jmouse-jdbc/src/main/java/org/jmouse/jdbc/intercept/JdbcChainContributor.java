package org.jmouse.jdbc.intercept;

import org.jmouse.core.chain.Chain;

@FunctionalInterface
public interface JdbcChainContributor {

    void contribute(Chain.Builder<JdbcExecutionContext, JdbcCall<?>, Object> builder);

}
