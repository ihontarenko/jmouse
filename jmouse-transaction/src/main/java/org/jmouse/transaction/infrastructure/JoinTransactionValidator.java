package org.jmouse.transaction.infrastructure;

import org.jmouse.core.Contract;
import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionIsolation;

import java.util.function.BiConsumer;

public final class JoinTransactionValidator {

    private final JoinAttributePolicy           isolationPolicy;
    private final JoinAttributePolicy           timeoutPolicy;
    private final JoinAttributePolicy           readOnlyPolicy;
    private final BiConsumer<String, Throwable> logger;

    public JoinTransactionValidator(
            JoinAttributePolicy isolationPolicy,
            JoinAttributePolicy timeoutPolicy,
            JoinAttributePolicy readOnlyPolicy,
            BiConsumer<String, Throwable> logger
    ) {
        this.isolationPolicy = Contract.nonNull(isolationPolicy, "isolationPolicy");
        this.timeoutPolicy = Contract.nonNull(timeoutPolicy, "timeoutPolicy");
        this.readOnlyPolicy = Contract.nonNull(readOnlyPolicy, "readOnlyPolicy");
        this.logger = Contract.nonNull(logger, "logger");
    }

    public void validate(TransactionDefinition requested, TransactionAttributes effective) {
        if (requested.getIsolation() != null && requested.getIsolation() != TransactionIsolation.DEFAULT) {
            if (requested.getIsolation() != effective.isolation()) {
                handlePolicy(isolationPolicy,
                             "Isolation mismatch on join: requested=%s, effective=%s".formatted(
                                     requested.getIsolation(), effective.isolation()
                             )
                );
            }
        }

        if (requested.isReadOnly() != effective.readOnly()) {
            handlePolicy(readOnlyPolicy,
                         "readOnly mismatch on join: requested=%s, effective=%s".formatted(
                                 requested.isReadOnly(), effective.readOnly()
                         )
            );
        }

        int timeoutSeconds = requested.getTimeoutSeconds();
        if (timeoutSeconds > 0) {
            if (!effective.hasTimeout()) {
                handlePolicy(timeoutPolicy,
                             "Timeout requested on join, but effective transaction has no timeout."
                );
            }
        }
    }

    private void handlePolicy(JoinAttributePolicy policy, String message) {
        switch (policy) {
            case IGNORE -> {}
            case WARNING -> logger.accept(message, null);
            case FAIL -> throw new IllegalStateException(message);
        }
    }
}
