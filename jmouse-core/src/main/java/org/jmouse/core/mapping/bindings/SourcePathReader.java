package org.jmouse.core.mapping.bindings;

import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.mapping.errors.MappingException;
import org.jmouse.core.mapping.model.SourceModel;
import org.jmouse.core.mapping.model.SourceModelFactory;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Objects;

/**
 * Internal path reader for mapping rules (3.2.1).
 * <p>
 * Reads values from Map/Bean/Record sources using {@link PropertyPath} segments.
 * No public "navigate" API is exposed.
 * </p>
 *
 * <p>Supported:</p>
 * <ul>
 *   <li>{@code a.b.c}</li>
 *   <li>{@code a.b[0].c}</li>
 * </ul>
 */
public final class SourcePathReader {

    private final SourceModelFactory sourceModelFactory;

    public SourcePathReader(SourceModelFactory sourceModelFactory) {
        this.sourceModelFactory = Objects.requireNonNull(sourceModelFactory, "sourceModelFactory");
    }

    public Object read(Object root, String path) {
        if (root == null) {
            return null;
        }

        if (path == null || path.isBlank()) {
            throw new MappingException("invalid_path", "Path must be non-empty");
        }

        PropertyPath         propertyPath = PropertyPath.forPath(path);
        PropertyPath.Entries entries      = propertyPath.entries();

        if (entries == null || entries.size() == 0) {
            return root;
        }

        Object current = root;

        for (int i = 0; i < entries.size(); i++) {
            if (current == null) {
                return null;
            }

            CharSequence      segment = entries.get(i);
            PropertyPath.Type type    = entries.type(i);

            if (type.isCorrupted()) {
                throw new MappingException(
                        "invalid_path", "Corrupted path segment at index %d in '%s'".formatted(i, path));
            }

            if (type.isIndexed()) {
                int index = parseIndex(segment, path, i);
                current = readIndex(current, index);
                continue;
            }

            String name = segment.toString();

            SourceModel model = sourceModelFactory.wrap(current);

            if (!model.has(name)) {
                return null;
            }

            current = model.read(name);
        }

        return current;
    }

    private int parseIndex(CharSequence segment, String path, int segmentIndex) {
        String text = segment.toString().trim();
        if (text.isEmpty()) {
            throw new MappingException("invalid_path", "Empty index at segment " + segmentIndex + " in '" + path + "'");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            throw new MappingException("invalid_path",
                                       "Invalid index '" + text + "' at segment " + segmentIndex + " in '" + path + "'",
                                       ex);
        }
    }

    private Object readIndex(Object current, int index) {
        if (current instanceof List<?> list) {
            if (index < 0 || index >= list.size()) {
                return null;
            }
            return list.get(index);
        }

        Class<?> type = current.getClass();
        if (type.isArray()) {
            int length = Array.getLength(current);
            if (index < 0 || index >= length) {
                return null;
            }
            return Array.get(current, index);
        }

        throw new MappingException("index_not_supported", "Index access is not supported for type: " + type.getName());
    }
}
