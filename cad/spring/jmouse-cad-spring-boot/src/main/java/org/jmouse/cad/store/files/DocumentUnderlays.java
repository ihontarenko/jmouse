package org.jmouse.cad.store.files;

import org.jmouse.cad.folder.StoredDocument;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.core.io.ByteArrayResource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 🖼️ The picture a drawing is drawn over — a part photographed from above for its portrait, a datasheet's package drawing for its
 * footprint, a pcb photographed for its outline —, kept with the drawing's document as two of its files ({@link DocumentFiles}): the
 * picture itself, and where it lies over the drawing. It goes wherever the document goes, and a reader who may see the document sees
 * it; only one who may file beside the document lays one, moves it or takes it away.
 */
public class DocumentUnderlays {

    /** The file that says where the picture lies, and what it is. */
    private static final String PLACEMENT_FILE = "underlay.json";
    /** The picture's own file is named so, its format's extension after it. */
    private static final String PICTURE_FILE = "underlay";

    /** A picture a drawing may be drawn over: the formats a browser shows as they are, and their files' extensions. */
    public enum PictureFormat {
        PNG("image/png", "png"), JPEG("image/jpeg", "jpg"), WEBP("image/webp", "webp"), GIF("image/gif", "gif");

        private final String mediaType;
        private final String extension;

        PictureFormat(String mediaType, String extension) {
            this.mediaType = mediaType;
            this.extension = extension;
        }

        public String mediaType() {
            return mediaType;
        }

        String fileName() {
            return PICTURE_FILE + "." + extension;
        }

        /** The format of a media type, as a request says it; refused, with the formats taken, for any other. */
        static PictureFormat of(String mediaType) {
            String type = mediaType == null ? "" : mediaType.split(";")[0].trim().toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(format -> format.mediaType.equals(type)).findFirst()
                    .orElseThrow(() -> CadProblem.invalid("A drawing is drawn over a picture: " + Arrays.stream(values())
                            .map(format -> format.extension.toUpperCase(Locale.ROOT)).toList() + ", not " + (type.isEmpty() ? "nothing" : type) + "."));
        }
    }

    /**
     * Where the picture lies over the drawing, in the drawing's own units: its top left corner and its width — its height follows
     * the picture —, how strongly it is drawn, and whether it is shown.
     *
     * @param aspect the picture's own height over its width
     */
    public record Placement(double x, double y, double width, double aspect, double opacity, boolean shown) {

        public Placement {
            if (!(width > 0) || !(aspect > 0) || !Double.isFinite(x) || !Double.isFinite(y)) {
                throw CadProblem.invalid("A picture lies somewhere, as wide as something and as tall as its own shape says.");
            }
            if (!(opacity >= 0 && opacity <= 1)) {
                throw CadProblem.invalid("A picture is drawn between not at all (0) and whole (1).");
            }
        }
    }

    /** The picture under a drawing: what it was called, its format, and where it lies. */
    public record Underlay(String name, PictureFormat format, Placement placement) {
    }

    /** The picture's bytes, with its format. */
    public record Picture(PictureFormat format, byte[] content) {
    }

    private final DocumentFiles  files;
    private final DocumentAccess access;
    private final JsonMapper     json = JsonMapper.builder().build();

    public DocumentUnderlays(DocumentFiles files, DocumentAccess access) {
        this.files = files;
        this.access = access;
    }

    /** The picture under a document's drawing; empty where it has none. */
    public Optional<Underlay> of(String address) {
        StoredDocument document = access.requireReadable(address);
        return underlayOf(document);
    }

    /** The picture's bytes; not found where the document has none. */
    public Picture picture(String address) {
        StoredDocument document = access.requireReadable(address);
        Underlay underlay = underlayOf(document).orElseThrow(() -> CadProblem.notFound("The picture under " + document.getSlug()));
        byte[] content = files.read(document.getId(), underlay.format().fileName(), DocumentFiles.LARGEST_FILE)
                .orElseThrow(() -> CadProblem.notFound("The picture under " + document.getSlug()));
        return new Picture(underlay.format(), content);
    }

    /** A picture laid under a document's drawing where it is placed, in place of the one it had. */
    public Underlay lay(String address, String name, String mediaType, byte[] content, Placement placement) {
        StoredDocument document = access.requireReadable(address);
        access.requireFilingBeside(document);
        PictureFormat format = PictureFormat.of(mediaType);
        if (content.length == 0) {
            throw CadProblem.invalid("The picture is empty.");
        }
        if (content.length > DocumentFiles.LARGEST_FILE) {
            throw CadProblem.invalid("The picture is larger than a document's file may be (" + DocumentFiles.LARGEST_FILE / (1024 * 1024) + " MB).");
        }
        // A picture of another format stands under another name: the one it replaces goes.
        underlayOf(document).filter(before -> before.format() != format).ifPresent(before -> files.remove(document.getId(), before.format().fileName()));
        files.store(document.getId(), format.fileName(), new ByteArrayResource(content));
        Underlay underlay = new Underlay(plainName(name), format, placement);
        keepPlacement(document, underlay);
        return underlay;
    }

    /** The picture under a document's drawing moved, made fainter or stronger, shown or hidden. */
    public Underlay place(String address, Placement placement) {
        StoredDocument document = access.requireReadable(address);
        access.requireFilingBeside(document);
        Underlay before = underlayOf(document).orElseThrow(() -> CadProblem.notFound("The picture under " + document.getSlug()));
        Underlay underlay = new Underlay(before.name(), before.format(), placement);
        keepPlacement(document, underlay);
        return underlay;
    }

    /** The picture taken away from under a document's drawing; nothing where it had none. */
    public void remove(String address) {
        StoredDocument document = access.requireReadable(address);
        access.requireFilingBeside(document);
        underlayOf(document).ifPresent(underlay -> {
            files.remove(document.getId(), underlay.format().fileName());
            files.remove(document.getId(), PLACEMENT_FILE);
        });
    }

    private Optional<Underlay> underlayOf(StoredDocument document) {
        return files.read(document.getId(), PLACEMENT_FILE, DocumentFiles.LARGEST_FILE)
                .map(bytes -> json.readValue(new String(bytes, StandardCharsets.UTF_8), Underlay.class));
    }

    private void keepPlacement(StoredDocument document, Underlay underlay) {
        files.store(document.getId(), PLACEMENT_FILE, new ByteArrayResource(json.writeValueAsString(underlay).getBytes(StandardCharsets.UTF_8)));
    }

    /** What the picture was called, as one plain name: whatever folders its path named are not the document's to keep. */
    private static String plainName(String name) {
        if (name == null || name.isBlank()) {
            return PICTURE_FILE;
        }
        String plain = name.replace('\\', '/');
        return plain.substring(plain.lastIndexOf('/') + 1).strip();
    }
}
