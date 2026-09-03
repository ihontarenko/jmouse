package org.jmouse.validator.management;

import org.jmouse.validator.el.JmvReader;
import org.jmouse.validator.el.builder.UnshowableValidationException;
import org.jmouse.validator.el.builder.ValidationDraft;
import org.jmouse.validator.el.builder.ValidationDrafts;
import org.jmouse.validator.el.parser.JmvSyntaxException;
import org.jmouse.validator.el.runtime.CheckSignature;
import org.jmouse.validator.el.runtime.CheckSignatures;
import org.jmouse.validator.el.translate.JmvWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * The validation builder's server half — the check catalogue, and the two directions between rows and a
 * document. 🧾
 *
 * <h2>⚠️ Why this is a library and not a screen inside a product</h2>
 *
 * <p>A validation builder is about a jMouse <em>language</em>, the way the mapping builder is. Built
 * inside one product it is a screen the next product copies — and a copied screen is a second
 * implementation of the rendering, which is the exact thing this whole feature is arranged to prevent.
 * So the library serves it and a product mounts it, gated by its own {@code ExternalAccessRules} like
 * every other management surface here.</p>
 *
 * <h2>⚠️ Rows in, text out — and nothing assembles the language but the writer</h2>
 *
 * <p>{@link #render} takes structure and answers text. It never takes text apart or puts text together:
 * it builds the document's own nodes and hands them to {@code JmvWriter}, which is the same call an
 * editor's save goes through. A browser that concatenated {@code "part_number : size(3, 32)"} would be
 * a second writer of the language, and the first thing a second writer gets wrong is quoting.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@RestController
@RequestMapping(ValidationBuilderRoutes.PREFIX)
public class ValidationBuilderController {

    private final CheckSignatures signatures;

    /** The vocabulary the library ships with, where a product contributes none of its own. */
    public ValidationBuilderController() {
        this(CheckSignatures.standard());
    }

    public ValidationBuilderController(CheckSignatures signatures) {
        this.signatures = signatures;
    }

    /**
     * What a form may offer, and how to draw each one.
     *
     * <h3>⚠️ This is what makes a validation drawable at all</h3>
     *
     * <p>A constraint binds its arguments <strong>by property name</strong>; a check writes them
     * <strong>by position</strong>, because {@code size(3, 32)} is what somebody wants to type. Without
     * this table a builder could offer a check but not label its inputs — which is a text box with a
     * name on it, and no better than the editor.</p>
     *
     * <p>⚠️ A <strong>listing</strong>, never a statement about what may be validated. A product that
     * registered a constraint of its own registers a signature for it, and it appears here beside the
     * shipped ones.</p>
     *
     * @return every check, with its positional parameter names in order
     */
    @GetMapping("/checks")
    public List<OfferedCheck> checks() {
        List<OfferedCheck> offered = new ArrayList<>();

        // ⚠️ `optional` is on the list although it builds no constraint. The word says something to a
        // reader — this field may be absent on purpose — and a builder that could not offer it would
        // make every optional field look like one nobody thought about.
        offered.add(new OfferedCheck(CheckSignatures.OPTIONAL, null, List.of(), false));

        for (CheckSignature signature : signatures.all()) {
            offered.add(new OfferedCheck(signature.check(), signature.constraint(),
                                         signature.positional(), signature.variadic()));
        }

        return offered;
    }

    /**
     * The form's rows as a document.
     *
     * @param draft what the form holds
     * @return the {@code .jmv} text, rendered by the one writer
     */
    @PostMapping("/render")
    public RenderedValidation render(@RequestBody ValidationDraft draft) {
        return new RenderedValidation(new JmvWriter().translate(ValidationDrafts.toDocument(draft)));
    }

    /**
     * A document as the form's rows.
     *
     * <p>⚠️ Refuses rather than narrows — see {@link UnshowableValidationException}. A form that showed
     * what it understood and dropped the rest would save, and the save would delete what it never
     * showed.</p>
     *
     * @param document the text somebody edited
     * @return the rows
     */
    @PostMapping("/parse")
    public ValidationDraft parse(@RequestBody RenderedValidation document) {
        return ValidationDrafts.toDraft(new JmvReader().parse(document.text(), "builder.jmv"));
    }

    /**
     * A document the form cannot show, reported as something a screen can act on.
     *
     * <p>⚠️ {@code 422} rather than {@code 400}: the request is well formed and the document is valid.
     * What cannot happen is showing it as rows, which is a statement about this screen and not about the
     * file — and the difference decides whether a person goes looking for a syntax error that is not
     * there.</p>
     *
     * @param unshowable what stopped it
     * @return the problem, carrying the construct on its own so a screen need not parse a sentence
     */
    @ExceptionHandler(UnshowableValidationException.class)
    public ProblemDetail unshowable(UnshowableValidationException unshowable) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, unshowable.getMessage());

        problem.setTitle("This validation can only be edited as text");
        problem.setProperty("construct", unshowable.construct());

        return problem;
    }

    /**
     * A document that will not parse.
     *
     * <p>⚠️ {@code 400} and the <strong>line</strong>, separately. Somebody is editing text and has
     * mistyped; a screen that can put the caret on the line is the difference between a message and a
     * hunt.</p>
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
     * One check a form may offer.
     *
     * @param check      the word a file writes
     * @param constraint what it builds, for a screen that wants to explain itself; {@code null} for a
     *                   word that builds nothing
     * @param parameters the property each positional argument fills, in order — the labels a form draws
     * @param variadic   whether the last one collects every remaining argument, which is what makes
     *                   {@code oneOf('SMD', 'THT')} a list input rather than two boxes
     */
    public record OfferedCheck(String check, String constraint, List<String> parameters,
                               boolean variadic) {
    }

    /**
     * A rendered document.
     *
     * <p>A record rather than a bare string, so the response has somewhere to grow — a hash, a warning,
     * the position of a construct — without every consumer changing shape.</p>
     *
     * @param text the {@code .jmv}
     */
    public record RenderedValidation(String text) {
    }
}
