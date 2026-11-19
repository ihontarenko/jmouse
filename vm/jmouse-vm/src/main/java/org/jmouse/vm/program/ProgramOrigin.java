package org.jmouse.vm.program;

import org.jmouse.el.InputUnit;

import java.util.Objects;

/** Half-open input range. Byte offsets and UTF-16 offsets remain distinct. */
public record ProgramOrigin(String source, InputUnit unit, long start, long end) {

    public ProgramOrigin {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(unit, "unit");
        if (source.isBlank() || start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid program origin");
        }
    }
}
