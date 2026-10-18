package org.jmouse.java.index.store.disk;

import java.util.List;

record IndexManifest(
        String projectRoot, String revisionKey, long modificationCount, List<String> segments) {
    IndexManifest {
        segments = List.copyOf(segments);
    }
}
