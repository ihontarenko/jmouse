package org.jmouse.datamap;

import java.util.Optional;

/** One line of an entry: a name, its value, and where the value was read. */
public record DataMapField(String name, DataMapValue value, Optional<DataMapSource> source) {

    public DataMapField(String name, DataMapValue value) {
        this(name, value, Optional.empty());
    }
}
