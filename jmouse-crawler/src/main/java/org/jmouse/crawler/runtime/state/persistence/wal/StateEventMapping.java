package org.jmouse.crawler.runtime.state.persistence.wal;

import org.jmouse.core.Verify;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StateEventMapping {

    private final Map<String, Class<? extends StateEvent>> byType;

    private StateEventMapping(Map<String, Class<? extends StateEvent>> byType) {
        this.byType = byType;
    }

    public Class<? extends StateEvent> resolve(String type) {
        Class<? extends StateEvent> clazz = byType.get(type);
        Verify.state(clazz != null, "Unknown StateEvent type: " + type);
        return clazz;
    }

    public Map<String, Class<? extends StateEvent>> asMap() {
        return byType;
    }

    public static StateEventMapping standard() {
        Map<String, Class<? extends StateEvent>> map = new LinkedHashMap<>();
        map.put(StateEvent.Types.FRONTIER_OFFERED, StateEvent.FrontierOffered.class);
        map.put(StateEvent.Types.FRONTIER_POLLED,  StateEvent.FrontierPolled.class);
        map.put(StateEvent.Types.RETRY_SCHEDULED,  StateEvent.RetryScheduled.class);
        map.put(StateEvent.Types.RETRY_DRAINED,    StateEvent.RetryDrained.class);
        map.put(StateEvent.Types.INFLIGHT_ADDED,   StateEvent.InFlightAdded.class);
        map.put(StateEvent.Types.INFLIGHT_REMOVED, StateEvent.InFlightRemoved.class);
        map.put(StateEvent.Types.SEEN_DISCOVERED,  StateEvent.SeenDiscovered.class);
        map.put(StateEvent.Types.SEEN_PROCESSED,   StateEvent.SeenProcessed.class);
        map.put(StateEvent.Types.DLQ_PUT,          StateEvent.DlqPut.class);
        return new StateEventMapping(Collections.unmodifiableMap(map));
    }

}
