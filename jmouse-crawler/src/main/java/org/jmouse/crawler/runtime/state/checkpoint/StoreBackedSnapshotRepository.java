package org.jmouse.crawler.runtime.state.checkpoint;

import org.jmouse.core.Verify;

public final class StoreBackedSnapshotRepository<S> implements SnapshotRepository<S> {

    private final FileSnapshotStore store;
    private final SnapshotCodec     codec;
    private final Class<S>          type;
    private final S                 empty;

    public StoreBackedSnapshotRepository(
            FileSnapshotStore store,
            SnapshotCodec codec,
            Class<S> type, S empty
    ) {
        this.store = Verify.nonNull(store, "store");
        this.codec = Verify.nonNull(codec, "codec");
        this.type  = Verify.nonNull(type, "type");
        this.empty = Verify.nonNull(empty, "empty");
    }

    @Override
    public S load() {
        String raw = store.tryRead();
        if (raw == null) {
            return empty;
        }
        try {
            S snapshot = codec.decode(raw, type);
            return (snapshot != null) ? snapshot : empty;
        } catch (Exception e) {
            return empty;
        }
    }

    @Override
    public void save(S snapshot) {
        String raw = codec.encode(Verify.nonNull(snapshot, "snapshot"));
        store.write(raw);
    }
}
