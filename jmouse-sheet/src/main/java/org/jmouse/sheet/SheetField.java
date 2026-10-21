package org.jmouse.sheet;

import java.util.Optional;

/** One line of an entry: a name, its value, where the value was read, and what the author wrote about it. */
public record SheetField(String name, SheetValue value, Optional<SheetSource> source, SheetComments comments) {

    public SheetField(String name, SheetValue value) {
        this(name, value, Optional.empty(), SheetComments.NONE);
    }
}
