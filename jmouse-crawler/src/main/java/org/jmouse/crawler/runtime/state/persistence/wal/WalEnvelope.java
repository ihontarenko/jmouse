package org.jmouse.crawler.runtime.state.persistence.wal;

public record WalEnvelope(String type, Object payload) {
    public static WalEnvelope of(StateEvent e) {
        return new WalEnvelope(e.type(), e);
    }
}