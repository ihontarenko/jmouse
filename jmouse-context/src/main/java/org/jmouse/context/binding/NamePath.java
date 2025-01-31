package org.jmouse.context.binding;

import org.jmouse.util.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringJoiner;

/**
 *
 * Example that can be resolved
 * <ul>
 * <li>{@code application.main.name}</li>
 * <li>{@code server.hosts[0].name}</li>
 * <li>{@code log[org.jmouse.core].level}</li>
 * </ul>
 * */
final public class NamePath {

    public static final char    SEPARATOR = '.';
    private             Entries entries;

    public NamePath(String name) {
        this(new Parser(name, SEPARATOR).parse());
    }

    public NamePath(Entries entries) {
        this.entries = entries;
    }

    public static NamePath of(String name) {
        return new NamePath(name);
    }

    public void suffix(String suffix) {
        entries = entries.merge(new Parser(suffix, SEPARATOR).parse());
    }

    public void prefix(String prefix) {
        entries = new Parser(prefix, SEPARATOR).parse().merge(entries);
    }

    public NamePath append(NamePath suffix) {
        return new NamePath(entries.merge(suffix.entries));
    }

    public NamePath prepend(NamePath prefix) {
        return new NamePath(prefix.entries.merge(entries));
    }

    public NamePath append(String entries) {
        return append(NamePath.of(entries));
    }

    public NamePath prepend(String entries) {
        return prepend(NamePath.of(entries));
    }

    public Entries entries() {
        return entries;
    }

    public String get(int index) {
        return entries.get(index).toString();
    }

    public String path() {
        return entries.toString();
    }

    @Override
    public String toString() {
        return entries.toString();
    }

    private static class Parser {

        private static final int DEFAULT_CAPACITY = 6;

        private final CharSequence sequence;
        private final char         separator;
        private       int          size;
        private       int[]        starts;
        private       int[]        ends;
        private       int[]        types;

        public Parser(CharSequence sequence, char separator) {
            this.sequence = sequence;
            this.separator = separator;
            this.size = 0;
            this.starts = new int[DEFAULT_CAPACITY];
            this.ends = new int[DEFAULT_CAPACITY];
            this.types = new int[DEFAULT_CAPACITY];
        }

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

        public int type(char c, int type) {
            int     updated = type;
            boolean numeric = c >= '0' && c <= '9';
            boolean alpha   = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
            boolean dashed  = c == '-';

            if (alpha || numeric || dashed) {
                updated |= Type.DEFAULT;

                if (numeric) {
                    updated |= Type.NUMERIC;
                }

                if (dashed) {
                    updated |= Type.DASHED;
                }
            }

            return updated;
        }

        private void expand() {
            if (this.starts.length == this.size) {
                this.starts = Arrays.expand(this.starts, this.size + DEFAULT_CAPACITY);
                this.ends = Arrays.expand(this.ends, this.size + DEFAULT_CAPACITY);
                this.types = Arrays.expand(this.types, this.size + DEFAULT_CAPACITY);
            }
        }

        private void entry(int start, int end, int type) {
            expand();
            this.starts[this.size] = start;
            this.ends[this.size] = end;
            this.types[this.size] = type;
            this.size++;
        }

    }

    public record Entries(int size, int[] starts, int[] ends, int[] types, CharSequence sequence)
            implements Iterator<CharSequence>, Iterable<CharSequence> {

        private static int position = 0;

        public CharSequence get(int index) {
            ensureIndexBounds(index);
            return sequence.subSequence(this.starts[index], ends[index]);
        }

        public CharSequence first() {
            return get(0);
        }

        public CharSequence last() {
            return get(size() - 1);
        }

        public boolean isLast(int index) {
            ensureIndexBounds(index);
            return index == size() - 1;
        }

        public Type type(int index) {
            ensureIndexBounds(index);
            return new Type(types[index]);
        }

        public int getLength(int index) {
            ensureIndexBounds(index);
            return this.ends[index] - this.starts[index];
        }

        public Entries merge(Entries other) {
            int    size      = this.size + other.size;
            char   c         = other.sequence.charAt(0);
            String separator = c == '[' ? "" : String.valueOf(SEPARATOR);

            int[] starts = new int[size];
            int[] ends   = new int[size];
            int[] types  = new int[size];

            System.arraycopy(this.starts, 0, starts, 0, this.size);
            System.arraycopy(this.ends, 0, ends, 0, this.size);
            System.arraycopy(this.types, 0, types, 0, this.size);

            int offset = sequence.length() + separator.length();
            for (int i = 0; i < other.size; i++) {
                int index = this.size + i;
                starts[index] = other.starts[i] + offset;
                ends[index] = other.ends[i] + offset;
                types[index] = other.types[i];
            }

            return new Entries(size, starts, ends, types, sequence + separator + other.sequence);
        }

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

        public Entries skip(int count) {
            return slice(count, size);
        }

        public Entries limit(int limit) {
            return slice(0, Math.min(limit, size));
        }

        public String toOriginal() {
            StringBuilder builder = new StringBuilder();

            reset();

            int counter = 0;
            for (CharSequence value : this) {
                Type type = type(counter);

                if (!type.isEmpty()) {
                    if (type.isIndexed()) {
                        if (!builder.isEmpty()) {
                            builder.deleteCharAt(builder.length() - 1);
                        }
                        builder.append('[');
                    }

                    builder.append(value);

                    if (type.isIndexed()) {
                        builder.append(']');
                    }

                    if (!isLast(counter)) {
                        builder.append(".");
                    }
                }

                counter++;
            }

            reset();

            return builder.toString();
        }

        private void ensureIndexBounds(int index) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
        }

        public void reset() {
            position = 0;
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = position < size;

            if (!hasNext) {
                reset();
            }

            return hasNext;
        }

        @Override
        public CharSequence next() {
            return get(position++);
        }

        @Override
        public Iterator<CharSequence> iterator() {
            return this;
        }

        @Override
        public String toString() {
            return toOriginal();
        }
    }

    public static class Type {

        public static final int EMPTY     = 0;
        public static final int DEFAULT   = 1 << 2;
        public static final int INDEXED   = DEFAULT << 2;
        public static final int NUMERIC   = INDEXED << 2;
        public static final int DASHED    = NUMERIC << 2;
        public static final int CORRUPTED = DASHED << 2;

        private final int mask;

        public Type(int mask) {
            this.mask = mask;
        }

        public boolean isEmpty() {
            return mask == EMPTY;
        }

        public boolean isDefault() {
            return (mask & DEFAULT) == DEFAULT;
        }

        public boolean isIndexed() {
            return (mask & INDEXED) != 0;
        }

        public boolean isNumeric() {
            return (mask & NUMERIC) != 0;
        }

        public boolean isDashed() {
            return (mask & DASHED) != 0;
        }

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
