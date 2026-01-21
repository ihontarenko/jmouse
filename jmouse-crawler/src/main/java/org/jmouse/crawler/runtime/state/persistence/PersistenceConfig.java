package org.jmouse.crawler.runtime.state.persistence;

import org.jmouse.core.Verify;

import java.nio.file.Path;

public record PersistenceConfig(
        Path directory,
        Durability durability,
        SnapshotPolicy snapshotPolicy,
        StateCodec codec
) {
    public PersistenceConfig {
        Verify.nonNull(directory, "directory");
        Verify.nonNull(durability, "durability");
        Verify.nonNull(snapshotPolicy, "snapshotPolicy");
        Verify.nonNull(codec, "codec");
    }
}
