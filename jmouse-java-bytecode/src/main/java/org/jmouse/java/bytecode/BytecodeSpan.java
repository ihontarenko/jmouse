package org.jmouse.java.bytecode;

public record BytecodeSpan(long start, long end) {
    public BytecodeSpan {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid bytecode source range");
        }
    }

    public long length() {
        return end - start;
    }
}
