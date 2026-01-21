package org.jmouse.crawler.runtime.state.persistence;

import org.jmouse.core.Verify;

import java.nio.file.Path;
import java.time.Duration;

public final class PersistentStores {

    private final PersistenceConfig config;

    private PersistentStores(PersistenceConfig config) {
        this.config = Verify.nonNull(config, "config");
    }

    public static PersistentStores files(Path directory) {
        PersistenceConfig cfg = new PersistenceConfig(
                directory,
                Durability.batched(256, Duration.ofMillis(200)),
                SnapshotPolicy.every(10_000).or(SnapshotPolicy.every(Duration.ofSeconds(30))),
                new SimpleLineCodec()
        );
        return new PersistentStores(cfg);
    }

    public PersistentStores durability(Durability durability) {
        return new PersistentStores(new PersistenceConfig(
                config.directory(), durability, config.snapshotPolicy(), config.codec()
        ));
    }

    public PersistentStores snapshot(SnapshotPolicy policy) {
        return new PersistentStores(new PersistenceConfig(
                config.directory(), config.durability(), policy, config.codec()
        ));
    }

    public PersistentStores codec(StateCodec codec) {
        return new PersistentStores(new PersistenceConfig(
                config.directory(), config.durability(), config.snapshotPolicy(), codec
        ));
    }

    public PersistenceConfig config() {
        return config;
    }
}
