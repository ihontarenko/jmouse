package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.store.formula.FormulaGuide;
import org.jmouse.parallax.store.formula.FormulaGuide.Interpretation;
import org.jmouse.parallax.store.formula.FormulaNotations;
import org.jmouse.parallax.store.formula.FormulaNotations.Rendering;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The jMF language: its reference, reading a typed formula, and printing formulas. Formula sets are collections of
 * catalogue objects ({@code /collections?purpose=FORMULA_SET}), not a store of their own.
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX + "/formula")
public class FormulaController {

    private final FormulaGuide guide;
    private final FormulaNotations notations;

    public FormulaController(FormulaGuide guide, FormulaNotations notations) {
        this.notations = notations;
        this.guide = guide;
    }

    public record SourceRequest(String source) {
    }

    public record RenderRequest(List<String> sources) {
    }

    @GetMapping("/reference")
    @PublicEndpoint("the language reference is documentation")
    public Map<String, Object> reference() {
        return guide.reference();
    }

    @PostMapping("/interpret")
    @PublicEndpoint("reading a formula parses and typesets it; nothing is computed or stored")
    public Interpretation interpret(@RequestBody SourceRequest request) {
        return guide.interpret(request.source());
    }

    /** Every formula given, printed at once as LaTeX, jMF and plain text; a refusal is an answer, not an error. */
    @PostMapping("/render")
    @PublicEndpoint("printing a formula parses it; nothing is computed or stored")
    public List<Rendering> render(@RequestBody RenderRequest request) {
        return notations.render(request.sources());
    }
}
