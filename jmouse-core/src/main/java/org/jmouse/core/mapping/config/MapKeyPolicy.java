package org.jmouse.core.mapping.config;

public enum MapKeyPolicy {
    STRINGIFY,   // key -> String.valueOf(key)
    FAIL,        // throw if key is not String
    SKIP         // skip entries with non-string keys
}