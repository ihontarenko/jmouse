package org.jmouse.mapper.management;

import org.jmouse.mapper.el.JmmReader;
import org.jmouse.mapper.el.builder.MappableTypes;
import org.jmouse.mapper.el.builder.MappingDraft;
import org.jmouse.mapper.el.builder.MappingDrafts;
import org.jmouse.mapper.el.builder.UnshowableMappingException;
import org.jmouse.mapper.el.translate.JmmSourceTranslator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The mapping builder's server half — the two selects, a pair's properties, and the two directions
 * between rows and a document. 🧾
 *
 * <h2>⚠️ Why this is a library and not a screen inside a product</h2>
 *
 * <p>A mapping builder is about a jMouse <em>language</em>, the way the query builder is. Built inside
 * one product it is a screen the next product copies — and a copied screen is a second implementation
 * of the rendering, which is the exact thing this whole feature is arranged to prevent. So the library
 * serves it and a product mounts it, gated by its own {@code ExternalAccessRules} like every other
 * management surface here.</p>
 *
 * <h2>⚠️ Rows in, text out — and nothing assembles the language but the translator</h2>
 *
 * <p>{@link #render} takes structure and answers text. It never takes text apart or puts text together:
 * it builds the document's own nodes and hands them to {@code JmmSourceTranslator}, which is the same
 * call an editor's save goes through. A browser that concatenated
 * {@code "reference : reference | trim | upper"} would be a second writer of the language, and the
 * first thing a second writer gets wrong is quoting.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@RestController
@RequestMapping(MappingBuilderRoutes.PREFIX)
public class MappingBuilderController {

    private final MappableTypeSource types;

    public MappingBuilderController(MappableTypeSource types) {
        this.types = types;
    }

    /**
     * What to offer in the two selects.
     *
     * <p>⚠️ A <strong>listing</strong>, never a statement about what may be mapped. The engine maps
     * whatever it is handed, so a type absent from here is still mappable — which is what
     * {@link #shape} is for.</p>
     *
     * @return the types this product offers
     */
    @GetMapping("/types")
    public List<MappableTypes.MappableType> offered() {
        return MappableTypes.offered(types.matcher(), types.baseClasses());
    }

    /**
     * What one type is made of.
     *
     * <p>Answers for a type whether or not {@link #offered} listed it — a caller must be able to name
     * one the scan did not find, or the builder invents a restriction the engine does not have.</p>
     *
     * @param type the type's qualified name — ⚠️ a nested one spelled the way a class loader spells it
     * @return its properties, each saying whether it may be read and whether it may be written
     */
    @GetMapping("/shape")
    public MappableTypes.MappableShape shape(@RequestParam String type) {
        return MappableTypes.named(type);
    }

    /**
     * The form's rows as a document.
     *
     * @param draft what the form holds
     * @return the `.jmm` text, rendered by the one translator
     */
    @PostMapping("/render")
    public RenderedMapping render(@RequestBody MappingDraft draft) {
        return new RenderedMapping(
                JmmSourceTranslator.INSTANCE.translate(MappingDrafts.toDocument(draft)));
    }

    /**
     * A document as the form's rows.
     *
     * <p>⚠️ Refuses rather than narrows — see {@link UnshowableMappingException}. A form that showed
     * what it understood and dropped the rest would save, and the save would delete what it never
     * showed.</p>
     *
     * @param document the text somebody edited
     * @return the rows
     */
    @PostMapping("/parse")
    public MappingDraft parse(@RequestBody RenderedMapping document) {
        return MappingDrafts.toDraft(new JmmReader().parse(document.text(), "builder.jmm"));
    }

    /**
     * A document the form cannot show, reported as something a screen can act on.
     *
     * <p>⚠️ {@code 422} rather than {@code 400}: the request is well formed and the document is valid.
     * What cannot happen is showing it as rows, which is a statement about this screen and not about
     * the file — and the difference decides whether a person goes looking for a syntax error that is
     * not there.</p>
     *
     * @param unshowable what stopped it
     * @return the problem, carrying the construct on its own so a screen need not parse a sentence
     */
    @ExceptionHandler(UnshowableMappingException.class)
    public ProblemDetail unshowable(UnshowableMappingException unshowable) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, unshowable.getMessage());

        problem.setTitle("This mapping can only be edited as text");
        problem.setProperty("construct", unshowable.construct());

        return problem;
    }

    /**
     * A rendered document.
     *
     * <p>A record rather than a bare string, so the response has somewhere to grow — a hash, a warning,
     * the position of a construct — without every consumer changing shape.</p>
     *
     * @param text the `.jmm`
     */
    public record RenderedMapping(String text) {
    }
}
