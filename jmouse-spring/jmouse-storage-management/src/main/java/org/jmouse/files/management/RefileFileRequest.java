package org.jmouse.files.management;

/**
 * 📦 Where a file should be filed instead.
 *
 * <p>⚠️ Both halves are required, and the kind is not defaulted from the file's current binding. A
 * request that named only an identifier would mean "move it to this thing, whatever kind of thing it
 * is" — which reads fine until a product has two kinds of owner sharing an identifier space.</p>
 *
 * @param ownerType what kind of thing should hold it
 * @param ownerId   which one
 */
public record RefileFileRequest(String ownerType, String ownerId) {
}
