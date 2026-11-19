package org.jmouse.java.index.binary;

/** UTF-16 coordinates in the generated API view, never coordinates in the classfile. */
public record BinaryTextRange(int start, int end) {
    public BinaryTextRange {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid binary API view range");
        }
    }
}
