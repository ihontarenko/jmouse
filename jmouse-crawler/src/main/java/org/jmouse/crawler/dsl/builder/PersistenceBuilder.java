package org.jmouse.crawler.dsl.builder;

import org.jmouse.crawler.runtime.state.persistence.*;
import org.jmouse.core.Verify;

import java.nio.file.Path;
import java.time.Duration;

public final class PersistenceBuilder {

    private Path           directory;
    private Durability     durability     = Durability.batched(256, Duration.ofMillis(200));
    private SnapshotPolicy snapshotPolicy = SnapshotPolicy.every(10_000)
            .or(SnapshotPolicy.every(Duration.ofSeconds(30)));
    private StateCodec     codec          = new SimpleLineCodec(); // replace later

    public PersistenceBuilder directory(Path dir) {
        this.directory = Verify.nonNull(dir, "dir");
        return this;
    }

    public PersistenceBuilder durability(Durability durability) {
        this.durability = Verify.nonNull(durability, "durability");
        return this;
    }

    public PersistenceBuilder snapshot(SnapshotPolicy snapshotPolicy) {
        this.snapshotPolicy = Verify.nonNull(snapshotPolicy, "snapshotPolicy");
        return this;
    }

    public PersistenceBuilder codec(StateCodec codec) {
        this.codec = Verify.nonNull(codec, "codec");
        return this;
    }

    public PersistenceConfig build() {
        Verify.state(directory != null, "persistence.dir must be configured");
        return new PersistenceConfig(directory, durability, snapshotPolicy, codec);
    }

}
