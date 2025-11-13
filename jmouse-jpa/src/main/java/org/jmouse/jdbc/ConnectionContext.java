package org.jmouse.jdbc;

import java.sql.Connection;

final public class ConnectionContext {

    public static final String SAVEPOINT_PREFIX = "SAVEPOINT_";

    private Connection currentConnection;
    private boolean    transactionActive = false;
    private Boolean    savepointSupported;
    private int        savepointCounter  = 0;

    public Connection getCurrentConnection() {
        return currentConnection;
    }

    public void setCurrentConnection(Connection currentConnection) {
        this.currentConnection = currentConnection;
    }

    public boolean isTransactionActive() {
        return transactionActive;
    }

    public void setTransactionActive(boolean transactionActive) {
        this.transactionActive = transactionActive;
    }

    public Boolean getSavepointSupported() {
        return savepointSupported;
    }

    public void setSavepointSupported(Boolean savepointSupported) {
        this.savepointSupported = savepointSupported;
    }

    public int getSavepointCounter() {
        return savepointCounter;
    }

    public void setSavepointCounter(int savepointCounter) {
        this.savepointCounter = savepointCounter;
    }

}
