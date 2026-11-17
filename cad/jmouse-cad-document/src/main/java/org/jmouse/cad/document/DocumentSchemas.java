package org.jmouse.cad.document;

import org.jmouse.schemas.SchemaStyle;
import org.jmouse.schemas.TypeSchemas;
import org.jmouse.schemas.WrittenSchema;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 📐 The JSON Schema of every kind of document a module brings: its model as the engine answers it — a pcb's {@code PcbDrawing}, a
 * symbol's {@code SymbolDrawing}, a circuit —, every type and field described by the Javadoc of the model, written by
 * {@link TypeSchemas} and kept as a file beside the module, {@code org/jmouse/cad/documents/<kind>.schema.json}, for a reader to
 * open and the interface's wire types to be generated from. Nothing of it is written by hand: a module's test holds each file to
 * what its model says now ({@link #mismatches}), and writes them afresh when asked ({@link #WRITING}).
 */
public final class DocumentSchemas {

    /** Where a module keeps its kinds' schemas, among its resources. */
    public static final String FOLDER = "org/jmouse/cad/documents/";

    /** The system property a module's test is told by to write its schemas afresh: {@code -Djmouse.documents.schemas=rewrite}. */
    public static final String WRITING = "jmouse.documents.schemas";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private DocumentSchemas() {
    }

    /** What a module's test does with the schemas kept: checks them against the models, or writes them afresh. */
    public enum Writing {

        /** Each kept schema held to what its model says now; one that differs is a mismatch. */
        CHECK,

        /** Each schema written afresh from its model. */
        REWRITE;

        /** As the test was told by {@link #WRITING}; {@link #CHECK} where it was told nothing. */
        public static Writing asked() {
            return Optional.ofNullable(System.getProperty(WRITING)).map(word -> valueOf(word.toUpperCase(Locale.ROOT))).orElse(CHECK);
        }
    }

    /** Where a kind's schema is kept, among a module's resources. */
    public static String resourceOf(DocumentKind kind) {
        return FOLDER + kind.word() + ".schema.json";
    }

    /** What a kind's schema is known by: an absolute URI, as a schema's {@code $id} must be — {@code urn:jmouse:cad:document:pcb}. */
    public static String idOf(DocumentKind kind) {
        return "urn:jmouse:cad:document:" + kind.word();
    }

    /** A kind's schema, written from its model, headed by where it is kept and what it is. */
    public static WrittenSchema of(DocumentKind kind, DocumentModel<?> model) {
        WrittenSchema written = TypeSchemas.write(model.type(), SchemaStyle.defined());
        ObjectNode schema = JSON.createObjectNode();
        schema.put("$schema", TypeSchemas.DRAFT);
        schema.put("$id", idOf(kind));
        schema.put("title", "A " + kind.word() + " document, as the engine answers it (" + TypeSchemas.nameOf(model.type()) + ")");
        written.schema().properties().stream()
               .filter(field -> !Map.of("$schema", 0, "title", 0).containsKey(field.getKey()))
               .forEach(field -> schema.set(field.getKey(), field.getValue()));
        return new WrittenSchema(schema, written.undescribed().stream().map(line -> kind.word() + ": " + line).toList());
    }

    /** A kind's schema kept on the class path; empty where its module keeps none. */
    public static Optional<ObjectNode> kept(DocumentKind kind) {
        try (InputStream kept = DocumentSchemas.class.getClassLoader().getResourceAsStream(resourceOf(kind))) {
            return kept == null ? Optional.empty() : Optional.of((ObjectNode) JSON.readTree(kept));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /** A schema as its file is written: two spaces a level, a line at its end, so a written file is the same bytes twice. */
    public static String text(ObjectNode schema) {
        return schema.toPrettyString().replace("\r\n", "\n") + "\n";
    }

    /**
     * What is wrong with a module's schemas, in words: a model its Javadoc does not describe, a kept file that is not what its model
     * says now, or none kept — written afresh first where the test was asked to.
     *
     * @param module    the module whose kinds are checked
     * @param resources the module's resources folder, {@code src/main/resources}
     * @param writing   whether the files are only checked or written afresh
     */
    public static List<String> mismatches(DocumentModule module, Path resources, Writing writing) {
        List<String> found = new ArrayList<>();
        for (Map.Entry<DocumentKind, DocumentModel<?>> model : module.models().entrySet()) {
            WrittenSchema written = of(model.getKey(), model.getValue());
            found.addAll(written.undescribed());
            Path file = resources.resolve(resourceOf(model.getKey()));
            String fresh = text(written.schema());
            try {
                if (writing == Writing.REWRITE) {
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, fresh, StandardCharsets.UTF_8);
                } else if (!Files.exists(file)) {
                    found.add(model.getKey().word() + ": no schema is kept at " + file + "; write it with -D" + WRITING + "=rewrite");
                } else if (!Files.readString(file, StandardCharsets.UTF_8).equals(fresh)) {
                    found.add(model.getKey().word() + ": " + file + " is not what its model says now; write it with -D" + WRITING + "=rewrite");
                }
            } catch (IOException unwritable) {
                found.add(model.getKey().word() + ": " + file + ": " + unwritable.getMessage());
            }
        }
        return found;
    }
}
