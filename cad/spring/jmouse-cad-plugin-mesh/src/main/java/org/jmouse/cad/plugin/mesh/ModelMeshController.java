package org.jmouse.cad.plugin.mesh;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.management.CadManagementRoutes;
import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.cad.store.showcase.FeaturedDesigns;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 🧊 A model as GLB, under the plugin's own prefix: millimetres, Z up, as a pcb is drawn. */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX + "/plugins/" + MeshPlugin.ID)
public class ModelMeshController {

    private static final MediaType GLB = MediaType.parseMediaType("model/gltf-binary");

    private final ModelMeshService meshes;
    private final FeaturedDesigns  featured;

    public ModelMeshController(ModelMeshService meshes, FeaturedDesigns featured) {
        this.meshes = meshes;
        this.featured = featured;
    }

    @GetMapping("/models/{address}/mesh")
    @PublicEndpoint("a model is answered only when the reader may see it, checked in the service")
    public ResponseEntity<byte[]> mesh(@PathVariable String address, @RequestParam(required = false) String owner) {
        return ResponseEntity.ok().contentType(GLB).cacheControl(CacheControl.noCache().cachePrivate())
                .body(meshes.glb(address, WrittenOwners.optional(owner)));
    }

    /** A model a featured design stands with, as the showcase shows its pcb in 3D to anybody. */
    @GetMapping("/showcase/{circuit}/models/{model}/mesh")
    @PublicEndpoint("only a 3D model one of a featured, published design's footprints names, checked by FeaturedDesigns")
    public ResponseEntity<byte[]> featuredMesh(@PathVariable String circuit, @PathVariable String model) {
        featured.requireShows(circuit, model);
        return ResponseEntity.ok().contentType(GLB).cacheControl(CacheControl.noCache()).body(meshes.glb(model, null));
    }
}
