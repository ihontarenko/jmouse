package org.jmouse.java.index.store.disk;

import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.FileSymbols;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32C;

/**
 * Closed record schema, not Java serialization. Input never names or loads an arbitrary class.
 * Strings and immutable record identities are shared to avoid quadratic storage of expression
 * trees.
 */
public final class IndexBinaryCodec {
    private IndexBinaryCodec() {}

    /** Closed-schema payload API shared by disk and JPA adapters; no filesystem I/O. */
    public static byte[] encodeFile(FileIndex file) throws IOException {
        return encode(file);
    }

    /** The closed record schema is part of every reusable contribution's identity. */
    public static String revision() {
        return VERSION + ":" + Integer.toUnsignedString(SCHEMA);
    }

    public static byte[] encodeCatalog(FileSymbols catalog) throws IOException {
        return encode(catalog);
    }

    public static FileSymbols decodeCatalog(byte[] bytes) throws IOException {
        Object decoded = decode(ByteBuffer.wrap(bytes));
        if (!(decoded instanceof FileSymbols catalog)) {
            throw new IOException("Payload does not contain FileSymbols.");
        }
        return catalog;
    }

    public static FileIndex decodeFile(byte[] bytes) throws IOException {
        Object decoded = decode(ByteBuffer.wrap(bytes));
        if (!(decoded instanceof FileIndex file)) {
            throw new IOException("Payload does not contain a FileIndex.");
        }
        return file;
    }

    private static final int MAGIC = 0x4A4D4958;
    static final int VERSION = 1;
    static final int MAXIMUM_BYTES = 128 * 1024 * 1024;
    private static final int MAXIMUM_DEPTH = 1024;
    private static final int MAXIMUM_ELEMENTS = 2_000_000;
    private static final int MINIMUM_FRAME_BYTES = 4 * Integer.BYTES;

    private enum Tag {
        NULL(0),
        STRING(1),
        STRING_REFERENCE(2),
        INTEGER(3),
        LONG(4),
        ENUM(5),
        LIST(6),
        SET(7),
        OBJECT_REFERENCE(8),
        RECORD(9),
        TRUE(10),
        FALSE(11);
        private final int identifier;

        Tag(int identifier) {
            this.identifier = identifier;
        }

        static Tag read(ByteBuffer input) throws IOException {
            int identifier = Byte.toUnsignedInt(input.get());
            for (Tag tag : values()) {
                if (tag.identifier == identifier) {
                    return tag;
                }
            }
            throw new IOException("Unknown index value tag: " + identifier);
        }
    }

    private static final List<Class<?>> TYPES;
    private static final Map<Class<?>, Integer> TYPE_IDENTIFIERS = new HashMap<>();
    private static final Map<Class<?>, Shape> SHAPES = new HashMap<>();
    private static final int SCHEMA;

    static {
        Set<Class<?>> discovered = new HashSet<>();
        for (Class<?> root :
                List.of(
                        FileIndex.class,
                        FileSymbols.class,
                        IndexEntry.class,
                        IndexManifest.class)) {
            discover(root, discovered);
        }
        TYPES = discovered.stream().sorted(Comparator.comparing(Class::getName)).toList();
        StringBuilder schema = new StringBuilder();
        try {
            for (int index = 0; index < TYPES.size(); index++) {
                Class<?> type = TYPES.get(index);
                TYPE_IDENTIFIERS.put(type, index);
                schema.append(type.getName());
                if (type.isRecord()) {
                    RecordComponent[] components = type.getRecordComponents();
                    Constructor<?> constructor =
                            type.getDeclaredConstructor(
                                    Arrays.stream(components)
                                            .map(RecordComponent::getType)
                                            .toArray(Class[]::new));
                    constructor.setAccessible(true);
                    List<Method> accessors = new ArrayList<>();
                    for (RecordComponent component : components) {
                        Method accessor = component.getAccessor();
                        accessor.setAccessible(true);
                        accessors.add(accessor);
                        schema.append(component.getName())
                                .append(component.getGenericType().getTypeName());
                    }
                    SHAPES.put(type, new Shape(constructor, accessors));
                } else {
                    schema.append(Arrays.toString(type.getEnumConstants()));
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        SCHEMA = schema.toString().hashCode();
    }

    private static void discover(Type type, Set<Class<?>> found) {
        if (type instanceof ParameterizedType parameterized) {
            for (Type argument : parameterized.getActualTypeArguments()) {
                discover(argument, found);
            }
        } else if (type instanceof Class<?> candidate
                && (candidate.isRecord() || candidate.isEnum())
                && found.add(candidate)) {
            if (candidate.isRecord()) {
                for (RecordComponent component : candidate.getRecordComponents()) {
                    discover(component.getGenericType(), found);
                }
            }
        }
    }

    static byte[] encode(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        output.writeInt(SCHEMA);
        new Writer(output).write(value, 0);
        output.flush();
        byte[] payload = bytes.toByteArray();
        if (payload.length > MAXIMUM_BYTES - Integer.BYTES) {
            throw new IOException("Index payload exceeds its byte limit.");
        }
        CRC32C checksum = new CRC32C();
        checksum.update(payload);
        output.writeInt((int) checksum.getValue());
        return bytes.toByteArray();
    }

    static Object decode(ByteBuffer input) throws IOException {
        if (input.remaining() < MINIMUM_FRAME_BYTES || input.remaining() > MAXIMUM_BYTES) {
            throw new IOException("Invalid index frame length.");
        }
        ByteBuffer payload = input.slice();
        payload.limit(payload.limit() - Integer.BYTES);
        CRC32C checksum = new CRC32C();
        checksum.update(payload.asReadOnlyBuffer());
        int expected = input.getInt(input.limit() - Integer.BYTES);
        if ((int) checksum.getValue() != expected
                || payload.getInt() != MAGIC
                || payload.getInt() != VERSION
                || payload.getInt() != SCHEMA) {
            throw new IOException("Index checksum, version or record schema does not match.");
        }
        try {
            Object result = new Reader(payload).read(0);
            if (payload.hasRemaining()) {
                throw new IOException("Unexpected trailing index bytes.");
            }
            return result;
        } catch (RuntimeException | ReflectiveOperationException exception) {
            throw new IOException("Invalid index record.", exception);
        }
    }

    private record Shape(Constructor<?> constructor, List<Method> accessors) {}

    private static final class Writer {
        private final DataOutputStream output;
        private final Map<String, Integer> strings = new HashMap<>();
        private final IdentityHashMap<Object, Integer> objects = new IdentityHashMap<>();

        Writer(DataOutputStream output) {
            this.output = output;
        }

        void write(Object value, int depth) throws IOException {
            if (depth > MAXIMUM_DEPTH) {
                throw new IOException("Index object nesting exceeds its limit.");
            }
            if (value == null) {
                output.writeByte(Tag.NULL.identifier);
            } else if (value instanceof String string) {
                Integer identifier = strings.get(string);
                if (identifier != null) {
                    output.writeByte(Tag.STRING_REFERENCE.identifier);
                    output.writeInt(identifier);
                } else {
                    output.writeByte(Tag.STRING.identifier);
                    strings.put(string, strings.size());
                    byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
                    output.writeInt(bytes.length);
                    output.write(bytes);
                }
            } else if (value instanceof Integer number) {
                output.writeByte(Tag.INTEGER.identifier);
                output.writeInt(number);
            } else if (value instanceof Long number) {
                output.writeByte(Tag.LONG.identifier);
                output.writeLong(number);
            } else if (value instanceof Boolean booleanValue) {
                output.writeByte((booleanValue ? Tag.TRUE : Tag.FALSE).identifier);
            } else if (value instanceof Enum<?> enumeration) {
                output.writeByte(Tag.ENUM.identifier);
                output.writeShort(TYPE_IDENTIFIERS.get(enumeration.getDeclaringClass()));
                output.writeShort(enumeration.ordinal());
            } else if (value instanceof List<?> list) {
                output.writeByte(Tag.LIST.identifier);
                output.writeInt(list.size());
                for (Object element : list) {
                    write(element, depth + 1);
                }
            } else if (value instanceof Set<?> set) {
                output.writeByte(Tag.SET.identifier);
                output.writeInt(set.size());
                for (Object element :
                        set.stream().sorted(Comparator.comparing(Object::toString)).toList()) {
                    write(element, depth + 1);
                }
            } else {
                Integer identifier = objects.get(value);
                if (identifier != null) {
                    output.writeByte(Tag.OBJECT_REFERENCE.identifier);
                    output.writeInt(identifier);
                    return;
                }
                Shape shape = SHAPES.get(value.getClass());
                if (shape == null) {
                    throw new IOException("Unsupported index value: " + value.getClass().getName());
                }
                objects.put(value, objects.size());
                output.writeByte(Tag.RECORD.identifier);
                output.writeShort(TYPE_IDENTIFIERS.get(value.getClass()));
                try {
                    for (Method accessor : shape.accessors()) {
                        write(accessor.invoke(value), depth + 1);
                    }
                } catch (ReflectiveOperationException exception) {
                    throw new IOException("Could not encode an index record.", exception);
                }
            }
        }
    }

    private static final class Reader {
        private final ByteBuffer input;
        private final List<String> strings = new ArrayList<>();
        private final List<Object> objects = new ArrayList<>();

        Reader(ByteBuffer input) {
            this.input = input;
        }

        Object read(int depth) throws IOException, ReflectiveOperationException {
            if (depth > MAXIMUM_DEPTH) {
                throw new IOException("Index object nesting exceeds its limit.");
            }
            Tag tag = Tag.read(input);
            return switch (tag) {
                case NULL -> null;
                case STRING -> {
                    int length = count(MAXIMUM_BYTES);
                    if (length > input.remaining()) {
                        throw new IOException("Index string exceeds its frame.");
                    }
                    byte[] bytes = new byte[length];
                    input.get(bytes);
                    String string = new String(bytes, StandardCharsets.UTF_8);
                    strings.add(string);
                    yield string;
                }
                case STRING_REFERENCE -> strings.get(input.getInt());
                case INTEGER -> input.getInt();
                case LONG -> input.getLong();
                case ENUM ->
                        TYPES.get(Short.toUnsignedInt(input.getShort()))
                                .getEnumConstants()[Short.toUnsignedInt(input.getShort())];
                case LIST, SET -> {
                    int count = count(MAXIMUM_ELEMENTS);
                    if (count > input.remaining()) {
                        throw new IOException("Index collection exceeds its frame.");
                    }
                    List<Object> list = new ArrayList<>(count);
                    for (int index = 0; index < count; index++) {
                        list.add(read(depth + 1));
                    }
                    yield tag == Tag.LIST
                            ? List.copyOf(list)
                            : Set.copyOf(new LinkedHashSet<>(list));
                }
                case OBJECT_REFERENCE -> {
                    Object reference = objects.get(input.getInt());
                    if (reference == null) {
                        throw new IOException("Cyclic or forward index reference.");
                    }
                    yield reference;
                }
                case RECORD -> {
                    Class<?> type = TYPES.get(Short.toUnsignedInt(input.getShort()));
                    Shape shape = SHAPES.get(type);
                    int identifier = objects.size();
                    objects.add(null);
                    Object[] components = new Object[shape.accessors().size()];
                    for (int index = 0; index < components.length; index++) {
                        components[index] = read(depth + 1);
                    }
                    Object record = shape.constructor().newInstance(components);
                    objects.set(identifier, record);
                    yield record;
                }
                case TRUE -> true;
                case FALSE -> false;
            };
        }

        private int count(int maximum) throws IOException {
            int count = input.getInt();
            if (count < 0 || count > maximum) {
                throw new IOException("Invalid index value length.");
            }
            return count;
        }
    }
}
