package org.jmouse.grabber.journal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmouse.grabber.Visit;
import org.jmouse.grabber.VisitKey;

/**
 * 🧠 A journal that lives for one run.
 *
 * <p>Enough for a run that finishes, for a smoke, and for the first half of a development loop. It
 * remembers nothing across processes, so a run started against this one always starts over — which is
 * the honest behaviour rather than a limitation to work around.</p>
 */
final class MemoryVisitJournal implements VisitJournal {

    private final Set<String>       seen      = ConcurrentHashMap.newKeySet();
    private final Set<String>       processed = ConcurrentHashMap.newKeySet();
    private final Map<String, Visit> outstanding = new ConcurrentHashMap<>();

    @Override
    public boolean seen(VisitKey key) {
        return seen.contains(key.value());
    }

    @Override
    public boolean markSeen(VisitKey key) {
        return seen.add(key.value());
    }

    @Override
    public boolean processed(String itemKey) {
        return processed.contains(itemKey);
    }

    @Override
    public void markProcessed(String itemKey) {
        processed.add(itemKey);
    }

    @Override
    public void remember(VisitKey key, Visit visit) {
        outstanding.put(key.value(), visit);
    }

    @Override
    public void forget(VisitKey key) {
        outstanding.remove(key.value());
    }

    @Override
    public List<Visit> pending() {
        return new ArrayList<>(outstanding.values());
    }

    @Override
    public void clear() {
        seen.clear();
        processed.clear();
        outstanding.clear();
    }

    @Override
    public long seenCount() {
        return seen.size();
    }

}
