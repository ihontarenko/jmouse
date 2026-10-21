package org.jmouse.parallax.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.parallax.kicad.KiCadSchematicExport;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.store.kicad.KiCadExports;
import org.jmouse.parallax.store.kicad.KiCadImports;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * KiCad in and out. In: a design the reader chose — its sheets' texts, read by the browser from the project's folder — imported
 * into one owner's place as jMX circuits, a sheet each, and the symbols they draw with. What the import could not carry
 * comes back in {@code notes}, part by part; the server reads no file itself. Out: a circuit's text — saved or not, as
 * an editor holds it — written as a {@code .kicad_sch}, with what KiCad could not say in {@code notes}.
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX)
public class KiCadController {

    private final KiCadImports imports;
    private final KiCadExports exports;

    public KiCadController(KiCadImports imports, KiCadExports exports) {
        this.imports = imports;
        this.exports = exports;
    }

    /** @param source a jMX circuit */
    public record ExportRequest(String source) {
    }

    /** @param fileName what the schematic is called: the circuit's address, {@code .kicad_sch} */
    public record ExportResponse(String fileName, String text, List<String> notes) {
    }

    /**
     * A circuit as a KiCad schematic. Nothing is stored; the components it names are read as the reader may see them.
     * A text that does not read is refused as {@code invalid-document} with its line and column; a circuit KiCad cannot
     * draw — one with no layout — as {@code invalid-conversion}.
     */
    @PostMapping("/kicad/export")
    @PublicEndpoint("converting a text the caller sent; components are read as the reader may see them")
    public ExportResponse exportDocument(@RequestBody ExportRequest request) {
        if (request == null || request.source() == null || request.source().isBlank()) {
            throw ParallaxProblem.invalid("A circuit is a jMX text.");
        }
        try {
            KiCadSchematicExport.Exported exported = exports.exportDocument(request.source());
            return new ExportResponse(exported.fileName(), exported.text(), exported.notes());
        } catch (ExperienceParseException refused) {
            throw ParallaxProblem.invalidDocument(refused.getDetail(), refused.getLine(), refused.getColumn());
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw ParallaxProblem.invalid("invalid-conversion", refused.getMessage());
        }
    }

    /**
     * @param root  the root schematic's file name, {@code LINEAR_500W_PS-1.kicad_sch}
     * @param files every sheet's text by its file name, the root's among them
     */
    public record ImportRequest(String root, Map<String, String> files) {
    }

    @PostMapping(ParallaxManagementRoutes.OWNER + "/kicad/import")
    @ResponseStatus(HttpStatus.CREATED)
    public KiCadImports.Imported importDesign(@OwnerAddress String owner, @RequestBody ImportRequest request) {
        return imports.importDesign(OwnerAddressArgumentResolver.ownerOf(owner), request == null ? null : request.root(),
                request == null ? null : request.files());
    }
}
