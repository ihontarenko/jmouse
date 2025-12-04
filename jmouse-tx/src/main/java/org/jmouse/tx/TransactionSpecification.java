package org.jmouse.tx;


/**
 * Transaction configuration object.
 * Immutable implementations are recommended. 🧾
 */
public interface TransactionSpecification {

    PropagationPolicy getPropagation();

    IsolationLevel getIsolation();

    int getTimeout(); // seconds; 0 = none

    boolean isReadOnly();

    String getName(); // optional identifier / debugging

}
