package org.jmouse.tx.synchronization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultSynchronizationRegistry
        implements SynchronizationRegistry {

    private final List<TransactionSynchronization> synchronizations =
            new ArrayList<>();

    @Override
    public void register(TransactionSynchronization synchronization) {
        synchronizations.add(synchronization);
    }

    @Override
    public List<TransactionSynchronization> getSynchronizations() {
        return Collections.unmodifiableList(synchronizations);
    }

    @Override
    public void clear() {
        synchronizations.clear();
    }
}
