package org.jmouse.validator.management;

import jakarta.transaction.Transactional;
import org.jmouse.validator.el.JmvReader;
import org.jmouse.validator.el.parser.JmvSyntaxException;
import org.jmouse.validator.jpa.ValidationDocument;
import org.jmouse.validator.jpa.ValidationDocumentRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The documents themselves — kept by the library, pointed at by a product. 🗄️
 *
 * <h2>⚠️ Why the store is a library's and not a product's</h2>
 *
 * <p>Innoventa was going to keep a document as a row in its own config bag, whose value column is
 * {@code VARCHAR(2048)}. A six-field document measures 927 bytes and a real form has fifteen fields, so
 * the first one written would not fit — and it would not fit on <em>save</em>, after somebody had typed
 * the rules. The deeper reason is that a config bag holds scalars, and a document wants to know when it
 * changed. A cell that grows into a table is a table somebody did not make in time.</p>
 *
 * <h2>⚠️ A document is addressed by its NAME, not by an identifier</h2>
 *
 * <p>{@code validation "innoventa/part" { … }} is the identity the language itself uses and the loader
 * resolves by. Addressing rows by a generated id instead would mean a product holding two names for one
 * thing — the one in the file and the one in the URL — and nothing keeping them in step.</p>
 *
 * <p>⚠️ And it travels as a <strong>query parameter</strong>, encoded, rather than in the path: a name
 * carries slashes by design, so a path would have to be a wildcard and every handler would slice the
 * URL its own way. The mapping builder passes a nested type name the same way, for the same reason.</p>
 *
 * <h2>⚠️ Saving parses first, and refuses text that is not a document</h2>
 *
 * <p>A store that kept whatever it was given would let a product write a file that cannot be loaded,
 * and the failure would arrive at the next boot — in a place with no connection to whoever saved it.
 * The reader is the same one the runtime uses, so what is accepted here is what will load.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@RestController
@RequestMapping(ValidationBuilderRoutes.PREFIX + "/documents")
public class ValidationDocumentController {

    private final ValidationDocumentRegistry documents;
    private final JmvReader                  reader;

    public ValidationDocumentController(ValidationDocumentRegistry documents) {
        this(documents, new JmvReader());
    }

    public ValidationDocumentController(ValidationDocumentRegistry documents, JmvReader reader) {
        this.documents = documents;
        this.reader = reader;
    }

    /**
     * Every document, by name.
     *
     * <p>⚠️ Without their source. A listing is scanned, and a screen listing forty documents does not
     * want forty files — which is also why a caller that wants one asks for it.</p>
     *
     * @return what is stored
     */
    @GetMapping
    public List<StoredDocument> listed() {
        return documents.all().stream().map(StoredDocument::withoutSource).toList();
    }

    /**
     * One document.
     *
     * @param name what it calls itself — ⚠️ carries slashes, so it is encoded into a query parameter
     * @return it, source included
     */
    @GetMapping("/one")
    public StoredDocument read(@RequestParam String name) {
        return documents.find(name)
                .map(StoredDocument::of)
                .orElseThrow(() -> new NoSuchElementException(name));
    }

    /**
     * One document by its identifier.
     *
     * <p>⚠️ Beside reading by name, not instead of it, because the two answer different questions. The
     * <strong>name</strong> is how the language addresses a document. The <strong>id</strong> is how
     * something that has already chosen one keeps hold of it — a pointer stored in another table, which
     * has to survive the document being renamed.</p>
     *
     * @param id the document's identifier
     * @return it, source included
     */
    @GetMapping("/by-id")
    public StoredDocument readById(@RequestParam String id) {
        return documents.byId(id)
                .map(StoredDocument::of)
                .orElseThrow(() -> new NoSuchElementException(id));
    }

    /**
     * Replaces what an existing document says, leaving its name and its identity alone.
     *
     * <p>⚠️ Renaming is deliberately not possible here. A document is addressed by its name in every
     * file that loads one, so a rename is somebody pointing an address at different rules — a decision,
     * not an edit, and this route would make it invisible.</p>
     *
     * @param id      the document's identifier
     * @param written the {@code .jmv}
     * @return the stored document
     */
    @PutMapping("/by-id")
    @Transactional
    public StoredDocument rewrite(@RequestParam String id, @RequestBody WrittenDocument written) {
        ValidationDocument document = documents.byId(id)
                .orElseThrow(() -> new NoSuchElementException(id));

        reader.parse(written.source(), document.getName());

        return StoredDocument.of(documents.write(document.getName(), written.source()));
    }

    /**
     * Writes a document, creating it or replacing what it says.
     *
     * @param name    what it calls itself
     * @param written the {@code .jmv}
     * @return the stored document
     */
    @PutMapping("/one")
    @Transactional
    public StoredDocument write(@RequestParam String name, @RequestBody WrittenDocument written) {
        // ⚠️ Parsed and thrown away. The point is the refusal, not the tree: what is stored is the text
        // somebody wrote, comments and all, and a store that kept a rendering of the parse would be the
        // second implementation of the language.
        reader.parse(written.source(), name);

        return StoredDocument.of(documents.write(name, written.source()));
    }

    /**
     * Removes a document.
     *
     * <p>⚠️ Nothing here asks whether a product still points at it. This library does not know what
     * points at a document and must not guess; a product that keeps a reference enforces it with its own
     * foreign key, where the database can refuse rather than a library hoping.</p>
     *
     * @param name what it calls itself
     */
    @DeleteMapping("/one")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(@RequestParam String name) {
        if (!documents.remove(name)) {
            throw new NoSuchElementException(name);
        }
    }

    /**
     * A document nobody has stored.
     *
     * @param absent what was asked for
     * @return the problem
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail absent(NoSuchElementException absent) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "No validation document is stored as '%s'".formatted(absent.getMessage()));

        problem.setTitle("No such validation document");

        return problem;
    }

    /**
     * Text that is not a document.
     *
     * <p>⚠️ Refused on the way in rather than kept and discovered at the next boot, and carrying the
     * line so a screen can put the caret on it.</p>
     *
     * @param malformed what stopped it
     * @return the problem
     */
    @ExceptionHandler(JmvSyntaxException.class)
    public ProblemDetail malformed(JmvSyntaxException malformed) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, malformed.problem());

        problem.setTitle("This is not a validation document");
        problem.setProperty("line", malformed.lineNumber());
        problem.setProperty("column", malformed.column());

        return problem;
    }

    /**
     * What is sent when a document is written.
     *
     * @param source the {@code .jmv}, as somebody wrote it
     */
    public record WrittenDocument(String source) {
    }

    /**
     * A stored document, on the wire.
     *
     * @param id        ⚠️ what a product POINTS at. A pointer stored in another table has to survive
     *                  the document being renamed, so it is the id that travels, not the name
     * @param name      what it calls itself
     * @param source    the {@code .jmv}, or {@code null} in a listing
     * @param createdAt when it was first written
     * @param updatedAt when it last changed
     */
    public record StoredDocument(String id, String name, String source, Instant createdAt,
                                 Instant updatedAt) {

        static StoredDocument of(ValidationDocument document) {
            return new StoredDocument(document.getId(), document.getName(), document.getSource(),
                                      document.getCreatedAt(), document.getUpdatedAt());
        }

        static StoredDocument withoutSource(ValidationDocument document) {
            return new StoredDocument(document.getId(), document.getName(), null,
                                      document.getCreatedAt(), document.getUpdatedAt());
        }
    }
}
