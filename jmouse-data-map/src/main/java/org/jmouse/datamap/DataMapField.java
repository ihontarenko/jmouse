package org.jmouse.datamap;

import java.util.Optional;

/** One line of an entry: a name, its value, where the value was read, and what the author wrote about it. */
public record DataMapField(String name, DataMapValue value, Optional<DataMapSource> source, DataMapComments comments) {

    public DataMapField(String name, DataMapValue value) {
        this(name, value, Optional.empty(), DataMapComments.NONE);
    }
}
