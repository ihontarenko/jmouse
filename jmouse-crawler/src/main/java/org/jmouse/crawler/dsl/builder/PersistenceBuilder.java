package org.jmouse.crawler.dsl.builder;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.state.persistence.*;

import java.nio.file.Path;
import java.time.Duration;

import static org.jmouse.crawler.runtime.state.persistence.SnapshotPolicy.every;

public final class PersistenceBuilder {

    private Path           directory;
    private Durability     durability     = Durability.batched(256, Duration.ofMillis(200));
    private SnapshotPolicy snapshotPolicy = SnapshotPolicy.or(every(Duration.ofSeconds(30)), every(10_000));
    private Codec          codec          = new NoopCodec();

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

    public PersistenceBuilder codec(Codec codec) {
        this.codec = Verify.nonNull(codec, "codec");
        return this;
    }

    public PersistenceConfig build() {
        Verify.state(directory != null, "persistence.directory must be configured");
        return new PersistenceConfig(directory, durability, snapshotPolicy, codec);
    }

}
