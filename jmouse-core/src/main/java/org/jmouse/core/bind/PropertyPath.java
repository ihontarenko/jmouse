package org.jmouse.core.bind;

import org.jmouse.util.Exceptions;
import org.jmouse.util.Streamable;
import org.jmouse.util.helper.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Represents a structured path name that can be used to navigate and manipulate
 * hierarchical properties. This class provides parsing, merging, and transformation
 * capabilities for structured property paths.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PropertyPath path = PropertyPath.of("server.hosts[0].name");
 * System.out.println(path.get(1)); // Outputs: "hosts"
 * }</pre>
 *
 * <p>Supported formats:</p>
 * <ul>
 *     <li>{@code application.main.name}</li>
 *     <li>{@code server.hosts[0].name}</li>
 *     <li>{@code log[org.jmouse.core].level}</li>
 * </ul>
 */
final public class PropertyPath {

    /** An empty PropertyPath instance. */
    public static final Entries EMPTY = new Entries(0, new int[0], new int[0], new int[0], "");

    /** The separator used in property paths (dot notation). */
    public static final char SEPARATOR = '.';

    private Entries entries;

    /**
     * Constructs a PropertyPath by parsing the given string.
     *
     * @param name the property path to parse
     */
    private PropertyPath(String name) {
        this(new Parser(name, SEPARATOR).parse());
    }

    /**
     * Constructs a PropertyPath using pre-parsed {@link Entries}.
     *
     * @param entries the parsed entries
     */
    private PropertyPath(Entries entries) {
        this.entries = entries;
    }

    /**
     * Returns an empty PropertyPath instance.
     *
     * @return an empty PropertyPath
     */
    public static PropertyPath empty() {
        return new PropertyPath(EMPTY);
    }

    /**
     * Creates a PropertyPath instance from a given name.
     *
     * @param name the property path to parse
     * @return a PropertyPath instance, or an empty instance if the input is null
     */
    public static PropertyPath forPath(String name) {
        return name == null ? empty() : new PropertyPath(name);
    }

    /**
     * Appends a suffix to the current PropertyPath.
     *
     * @param suffix the path segment to append
     */
    public void suffix(String suffix) {
        entries = entries.merge(new Parser(suffix, SEPARATOR).parse());
    }

    /**
     * Prepends a prefix to the current PropertyPath.
     *
     * @param prefix the path segment to prepend
     */
    public void prefix(String prefix) {
        entries = new Parser(prefix, SEPARATOR).parse().merge(entries);
    }

    /**
     * Creates a new PropertyPath by appending another PropertyPath.
     *
     * @param suffix the PropertyPath to append
     * @return a new PropertyPath with the suffix added
     */
    public PropertyPath append(PropertyPath suffix) {
        return new PropertyPath(entries.merge(suffix.entries));
    }

    /**
     * Creates a new PropertyPath by prepending another PropertyPath.
     *
     * @param prefix the PropertyPath to prepend
     * @return a new PropertyPath with the prefix added
     */
    public PropertyPath prepend(PropertyPath prefix) {
        return new PropertyPath(prefix.entries.merge(entries));
    }

    /**
     * Creates a new PropertyPath by appending a property path string.
     *
     * @param entries the property path string to append
     * @return a new PropertyPath with the appended entries
     */
    public PropertyPath append(String entries) {
        return append(PropertyPath.forPath(entries));
    }

    /**
     * Creates a new PropertyPath by prepending a property path string.
     *
     * @param entries the property path string to prepend
     * @return a new PropertyPath with the prepended entries
     */
    public PropertyPath prepend(String entries) {
        return prepend(PropertyPath.forPath(entries));
    }

    /**
     * Retrieves the entries of this PropertyPath.
     *
     * @return the path entries
     */
    public Entries entries() {
        return entries;
    }

    /**
     * Gets the path segment at the specified index.
     *
     * @param index the index of the path segment
     * @return the path segment as a string
     */
    public String get(int index) {
        return entries.get(index).toString();
    }

    /**
     * Returns the full path as a string.
     *
     * @return the full property path
     */
    public String path() {
        return entries.toString();
    }

    @Override
    public String toString() {
        return entries.toString();
    }

    /**
     * Parses a {@link CharSequence} into structured {@link Entries} based on a given separator.
     * Supports detection of indexed elements (e.g., `[0]`), numeric values, and dashed entries.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Parser parser = new Parser("a.b[0].c-d", '.');
     * Entries entries = parser.parse();
     * System.out.println(entries); // Parsed representation of "a.b[0].c-d"
     * }</pre>
     */
    private static class Parser {

        /** Default initial capacity for storing parsed entries. */
        private static final int DEFAULT_CAPACITY = 6;

        private final CharSequence sequence;
        private final char         separator;
        private       int          size;
        private       int[]        starts;
        private       int[]        ends;
        private       int[]        types;

        /**
         * Constructs a new parser instance for a given sequence and separator.
         *
         * @param sequence  the character sequence to parse
         * @param separator the character used as a separator in the sequence
         */
        public Parser(CharSequence sequence, char separator) {
            this.sequence = sequence;
            this.separator = separator;
            this.size = 0;
            this.starts = new int[DEFAULT_CAPACITY];
            this.ends = new int[DEFAULT_CAPACITY];
            this.types = new int[DEFAULT_CAPACITY];
        }

        /**
         * Parses the sequence into structured entries, recognizing separators, indices, and special characters.
         *
         * @return an {@link Entries} object representing the parsed components
         */
        public Entries parse() {
            int        length    = sequence.length();
            final char separator = this.separator;
            int        opens     = 0;
            int        offset    = 0;
            int        type      = Type.EMPTY;

            for (int position = 0; position < length; position++) {
                char c = sequence.charAt(position);

                switch (c) {
                    case '[': {
                        if (opens == 0) {
                            if (offset < position) {
                                entry(offset, position, type);
                            }
                            offset = position + 1;
                            type = Type.INDEXED;
                        }
                        opens++;
                        break;
                    }
                    case ']': {
                        opens--;
                        if (opens == 0) {
                            entry(offset, position, type);
                            offset = position + 1;
                            type = Type.EMPTY;
                        }
                        break;
                    }
                    default: {
                        if ((type & Type.INDEXED) == 0 && c == separator) {
                            entry(offset, position, type);
                            offset = position + 1;
                            type = Type.DEFAULT;
                        } else {
                            type = type(c, type);
                        }
                        break;
                    }
                }
            }

            if (opens != 0) {
                type |= Type.CORRUPTED;
            }

            entry(offset, length, type);

            // remove unnecessary `Type.EMPTY`
            if (size > 1 && types[size - 1] == Type.EMPTY) {
                size--;
            }

            return new Entries(this.size, starts, ends, types, sequence);
        }

        /**
         * Determines the type of a character and updates the existing type bitmask.
         * Recognizes numeric, alphabetical, and dashed values.
         *
         * @param c    the character to evaluate
         * @param type the current bitmask representing the entry type
         * @return an updated bitmask including the new character type
         */
        public int type(char c, int type) {
            int     updated = type;
            boolean numeric = c >= '0' && c <= '9';
            boolean alpha   = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
            boolean dashed  = c == '-';
            boolean underscored  = c == '_';

            if (alpha || numeric || dashed || underscored) {
                if (alpha) {
                    updated |= Type.DEFAULT;
                }

                if (numeric && (type & Type.DEFAULT) == 0) {
                    updated |= Type.NUMERIC;
                }

                if (dashed) {
                    updated |= Type.DASHED;
                }

                if (underscored) {
                    updated |= Type.UNDERSCORED;
                }
            }

            return updated;
        }

        /**
         * Expands internal arrays when needed to accommodate additional parsed entries.
         */
        private void expand() {
            if (this.starts.length == this.size) {
                this.starts = Arrays.expand(this.starts, this.size + DEFAULT_CAPACITY);
                this.ends = Arrays.expand(this.ends, this.size + DEFAULT_CAPACITY);
                this.types = Arrays.expand(this.types, this.size + DEFAULT_CAPACITY);
            }
        }

        /**
         * Adds a new parsed entry with its start and end positions and type.
         *
         * @param start the starting index of the entry
         * @param end   the ending index of the entry
         * @param type  the type of the entry as a bitmask
         */
        private void entry(int start, int end, int type) {
            expand();
            this.starts[this.size] = start;
            this.ends[this.size] = end;
            this.types[this.size] = type;
            this.size++;
        }

    }

    /**
     * Represents a structured sequence of parsed path entries from a {@link PropertyPath}.
     * This record provides methods for accessing, merging, slicing, and iterating over path segments.
     *
     * <p>Entries can be sliced, merged, and iterated efficiently.</p>
     *
     * @param size      the number of parsed segments
     * @param starts    array of start positions for each segment
     * @param ends      array of end positions for each segment
     * @param types     array of type identifiers for each segment
     * @param sequence  the original sequence being parsed
     */
    public record Entries(int size, int[] starts, int[] ends, int[] types, CharSequence sequence)
            implements Iterator<CharSequence>, Streamable<CharSequence> {

        private static int position = 0;

        /**
         * Retrieves the segment at the given index.
         *
         * @param index the index of the segment
         * @return the segment as a {@link CharSequence}
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        public CharSequence get(int index) {
            ensureIndexBounds(index);
            return sequence.subSequence(this.starts[index], ends[index]);
        }

        /**
         * Returns the first segment in the path.
         *
         * @return the first path segment
         */
        public CharSequence first() {
            return get(0);
        }

        /**
         * Returns the last segment in the path.
         *
         * @return the last path segment
         */
        public CharSequence last() {
            return get(size() - 1);
        }

        /**
         * Checks if the given index corresponds to the last segment in the path.
         *
         * @param index the index to check
         * @return true if it is the last segment, false otherwise
         */
        public boolean isLast(int index) {
            ensureIndexBounds(index);
            return index == size() - 1;
        }

        /**
         * Retrieves the type of the segment at the given index.
         *
         * @param index the index of the segment
         * @return the type of the segment as {@link Type}
         */
        public Type type(int index) {
            ensureIndexBounds(index);
            return new Type(types[index]);
        }

        /**
         * Gets the length of the segment at the given index.
         *
         * @param index the index of the segment
         * @return the length of the segment
         */
        public int getLength(int index) {
            ensureIndexBounds(index);
            return this.ends[index] - this.starts[index];
        }

        /**
         * Merges this set of entries with another, returning a new combined {@code Entries} instance.
         *
         * @param other the entries to merge with
         * @return a new {@code Entries} instance representing the merged path
         */
        public Entries merge(Entries other) {
            int    size      = this.size + other.size;
            char   c         = other.sequence.isEmpty() ? Character.MIN_VALUE : other.sequence.charAt(0);
            String separator = c == '[' ? "" : String.valueOf(SEPARATOR);

            int[] starts = new int[size];
            int[] ends   = new int[size];
            int[] types  = new int[size];

            System.arraycopy(this.starts, 0, starts, 0, this.size);
            System.arraycopy(this.ends, 0, ends, 0, this.size);
            System.arraycopy(this.types, 0, types, 0, this.size);

            int offset = this.size > 0 ? sequence.length() + separator.length() : 0;
            for (int i = 0; i < other.size; i++) {
                int index = this.size + i;
                starts[index] = other.starts[i] + offset;
                ends[index] = other.ends[i] + offset;
                types[index] = other.types[i];
            }

            if (sequence.isEmpty() || other.sequence.isEmpty()) {
                separator = "";
            }

            return new Entries(size, starts, ends, types, sequence + separator + other.sequence);
        }

        /**
         * Creates a sub-sequence of entries within the given range.
         *
         * @param offset the start index (inclusive)
         * @param limit  the end index (exclusive)
         * @return a new {@code Entries} instance representing the sliced subset
         * @throws IllegalArgumentException if the range is invalid
         */
        public Entries slice(int offset, int limit) {
            ensureIndexBounds(offset);
            ensureIndexBounds(limit - 1);

            int size = limit - offset;

            if (size < 0) {
                throw new IllegalArgumentException(
                        "Invalid range: start(%d) must be less or equals end(%d)"
                                .formatted(offset, limit));
            }

            int[] starts = new int[size];
            int[] ends   = new int[size];
            int[] types  = new int[size];

            System.arraycopy(this.starts, offset, starts, 0, size);
            System.arraycopy(this.ends, offset, ends, 0, size);
            System.arraycopy(this.types, offset, types, 0, size);

            return new Entries(size, starts, ends, types, sequence);
        }

        /**
         * Skips the given number of entries from the beginning.
         *
         * @param count the number of entries to skip
         * @return a new {@code Entries} instance with skipped elements
         */
        public Entries skip(int count) {
            return slice(count, size);
        }

        /**
         * Limits the number of entries to the given count.
         *
         * @param limit the maximum number of entries to keep
         * @return a new {@code Entries} instance with at most {@code limit} elements
         */
        public Entries limit(int limit) {
            return slice(0, Math.min(limit, size));
        }

        /**
         * Restores the original sequence as a string.
         *
         * @return the original parsed sequence
         */
        public String toOriginal() {
            return sequence.toString();
        }

        private void ensureIndexBounds(int index) {
            if (index < 0 || index >= size()) {
                Exceptions.throwIfOutOfRange(index, 0, size, "Index: %d, Size: %d".formatted(index, size));
            }
        }

        /**
         * Resets the iterator position to the beginning.
         */
        public void reset() {
            position = 0;
        }

        /**
         * Checks if more elements are available in the iterator.
         *
         * @return true if there are more elements, false otherwise
         */
        @Override
        public boolean hasNext() {
            boolean hasNext = position < size;

            if (!hasNext) {
                reset();
            }

            return hasNext;
        }

        /**
         * Retrieves the next path segment in the iteration.
         *
         * @return the next segment
         */
        @Override
        public CharSequence next() {
            return get(position++);
        }

        /**
         * Returns an iterator over the path segments.
         *
         * @return an iterator over path segments
         */
        @Override
        public Iterator<CharSequence> iterator() {
            return this;
        }

        @Override
        public String toString() {
            return toOriginal();
        }
    }

    /**
     * Represents the type of entry in a {@link PropertyPath}.
     * Each type is defined as a bitmask and can be combined to describe multiple properties.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Type type = new Type(Type.INDEXED | Type.NUMERIC);
     * System.out.println(type.isIndexed()); // true
     * System.out.println(type.isNumeric()); // true
     * }</pre>
     */
    public static class Type {

        /** Represents an empty type. */
        public static final int EMPTY = 0;

        /** Represents a default (untyped) entry. */
        public static final int DEFAULT = 1 << 2;

        /** Indicates that the entry is indexed (e.g., array notation like [0]). */
        public static final int INDEXED = DEFAULT << 2;

        /** Indicates that the entry is numeric. */
        public static final int NUMERIC = INDEXED << 2;

        /** Indicates that the entry is dashed (e.g., contains hyphens). */
        public static final int DASHED = NUMERIC << 2;

        /** Indicates that the entry is underscored (e.g., contains underscore). */
        public static final int UNDERSCORED = DASHED << 2;

        /** Represents a corrupted entry (invalid or malformed data). */
        public static final int CORRUPTED = UNDERSCORED << 2;

        private final int mask;

        /**
         * Creates a new {@code Type} with the given bitmask.
         *
         * @param mask the bitmask representing the type
         */
        public Type(int mask) {
            this.mask = mask;
        }

        /**
         * Checks if the type is empty.
         *
         * @return true if the type is empty, false otherwise
         */
        public boolean isEmpty() {
            return mask == EMPTY;
        }

        /**
         * Checks if the type is default (untyped).
         *
         * @return true if the type is default, false otherwise
         */
        public boolean isDefault() {
            return (mask & DEFAULT) == DEFAULT;
        }

        /**
         * Checks if the entry is indexed (e.g., part of an array).
         *
         * @return true if the entry is indexed, false otherwise
         */
        public boolean isIndexed() {
            return (mask & INDEXED) != 0;
        }

        /**
         * Checks if the entry is numeric.
         *
         * @return true if the entry is numeric, false otherwise
         */
        public boolean isNumeric() {
            return (mask & NUMERIC) != 0;
        }

        /**
         * Checks if the entry contains dashes.
         *
         * @return true if the entry contains dashes, false otherwise
         */
        public boolean isDashed() {
            return (mask & DASHED) != 0;
        }

        /**
         * Checks if the entry contains underscore.
         *
         * @return true if the entry contains underscores, false otherwise
         */
        public boolean isUnderscored() {
            return (mask & UNDERSCORED) != 0;
        }

        /**
         * Checks if the entry is corrupted.
         *
         * @return true if the entry is corrupted, false otherwise
         */
        public boolean isCorrupted() {
            return (mask & CORRUPTED) != 0;
        }

        @Override
        public String toString() {
            StringBuilder        builder = new StringBuilder();

            if (isEmpty()) {
                builder.append("[EMPTY]");
            } else {
                StringJoiner         joiner = new StringJoiner(" & ", "[", "]");
                Map<String, Integer> masks  = new HashMap<>();

                masks.put("DEFAULT", DEFAULT);
                masks.put("INDEXED", INDEXED);
                masks.put("NUMERIC", NUMERIC);
                masks.put("DASHED", DASHED);
                masks.put("UNDERSCORED", UNDERSCORED);
                masks.put("CORRUPTED", CORRUPTED);

                for (Map.Entry<String, Integer> entry : masks.entrySet()) {
                    if ((mask & entry.getValue()) != 0) {
                        joiner.add(entry.getKey());
                    }
                }

                builder.append(joiner);
            }

            return builder.toString();
        }
    }

}
