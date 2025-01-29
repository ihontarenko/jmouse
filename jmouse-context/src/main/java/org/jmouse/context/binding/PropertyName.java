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
final public class PropertyName {

    private final Elements elements;

    public PropertyName(String name) {
        this.elements = new Parser(name, '.').parse();
    }

    public Elements getElements() {
        return elements;
    }

    public String getElement(int index) {
        return elements.get(index).toString();
    }

    public static PropertyName of(String name) {
        return new PropertyName(name);
    }

    private static class Parser {

        private static final int DEFAULT_CAPACITY = 6;

        private final CharSequence sequence;
        private final char  separator;
        private       int   size;
        private       int[] starts;
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

        public Elements parse() {
            int        length    = sequence.length();
            final char separator = this.separator;
            int        opens     = 0;
            int        offset    = 0;
            int        type      = Type.DEFAULT;

            for (int position = 0; position < length; position++) {
                char c = sequence.charAt(position);

                switch (c) {
                    case '[': {
                        if (opens == 0) {
                            entry(offset, position, type);
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
                        if ((type & Type.INDEXED) == 0 && sequence.charAt(position) == separator) {
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
                type = Type.CORRUPTED;
            }

            entry(offset, length, type);

            return new Elements(this.size, starts, ends, types, sequence);
        }

        public int type(char c, int type) {
            int updated = type;

            if (c >= '0' && c <= '9') {
                updated |= Type.NUMERIC;
            }

            if (c == '-') {
                updated |= Type.DASHED;
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

    public record Elements(int size, int[] starts, int[] ends, int[] types, CharSequence sequence)
            implements Iterator<CharSequence>, Iterable<CharSequence> {

        private static int position = 0;

        public CharSequence get(int index) {
            ensureIndexBounds(index);
            return sequence.subSequence(this.starts[index], ends[index]);
        }

        public CharSequence getFirst() {
            return get(0);
        }

        public CharSequence getLast() {
            return get(getSize() - 1);
        }

        public boolean isLast(int index) {
            ensureIndexBounds(index);
            return index == getSize() - 1;
        }

        public Type getType(int index) {
            ensureIndexBounds(index);
            return new Type(types[index]);
        }

        public int getSize() {
            return size;
        }

        public int getLength(int index) {
            ensureIndexBounds(index);
            return this.ends[index] - this.starts[index];
        }

        public Elements slice(int offset, int limit) {
            ensureIndexBounds(offset);
            ensureIndexBounds(limit - 1);

            int size = limit - offset;

            if (size < 0) {
                throw new IllegalArgumentException("Invalid range: start must be <= end");
            }

            int[] starts = new int[size];
            int[] ends   = new int[size];
            int[] types  = new int[size];

            System.arraycopy(this.starts, offset, starts, 0, size);
            System.arraycopy(this.ends, offset, ends, 0, size);
            System.arraycopy(this.types, offset, types, 0, size);

            return new Elements(size, starts, ends, types, sequence);
        }

        public Elements skip(int count) {
            return slice(count, size);
        }

        public Elements limit(int limit) {
            return slice(0, Math.min(limit, size));
        }

        public String toOriginal() {
            StringBuilder builder = new StringBuilder();

            position(0);
            int counter = 0;
            for (CharSequence value : this) {
                Type type = getType(counter);

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
            position(0);


            return builder.toString();
        }

        private void ensureIndexBounds(int index) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
        }

        public void position(int position) {
            this.position = position;
        }

        @Override
        public boolean hasNext() {
            return position < size;
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
