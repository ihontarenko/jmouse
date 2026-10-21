package org.jmouse.parallax.management;

import org.jmouse.parallax.store.kicad.KiCadImports;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * KiCad in: a design the reader chose — its sheets' texts, read by the browser from the project's folder — imported
 * into one owner's place as jMX circuits, a sheet each, and the symbols they draw with. What the import could not carry
 * comes back in {@code notes}, part by part; the server reads no file itself.
 */
@RestController
@RequestMapping(ParallaxManagementRoutes.PREFIX)
public class KiCadController {

    private final KiCadImports imports;

    public KiCadController(KiCadImports imports) {
        this.imports = imports;
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
