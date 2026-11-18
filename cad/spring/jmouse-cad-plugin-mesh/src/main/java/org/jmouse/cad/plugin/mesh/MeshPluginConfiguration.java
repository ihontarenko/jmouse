package org.jmouse.cad.plugin.mesh;

import org.jmouse.cad.table.TableSource;

import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbSolidLayers;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.showcase.FeaturedDesigns;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.spring.PluginAutoConfiguration;
import org.jmouse.strokefont.StrokeFont;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 🧩 The 3D models plugin, on the classpath: the plugin and the pcb's GLB export, model documents as their files, the
 * service that serves a model as GLB, and its route.
 */
@AutoConfiguration(after = PluginAutoConfiguration.class)
public class MeshPluginConfiguration {

    /** The places models come from are read from the plugins on each fetch, so the plugin needs none at its start. */
    @Bean
    public MeshPlugin cadMeshPlugin(ObjectProvider<Extensions> extensions, ObjectProvider<ModelMeshService> meshes, CadSettings sections, CadStandards standards) {
        PcbGlbExport pcb = new PcbGlbExport(() -> sections.read(MeshPlugin.GLB), () -> sections.read(PcbArtwork.SECTION),
                () -> sections.read(PcbSolidLayers.SECTION), StrokeFont.newStroke(), meshes::getObject, standards);
        return new MeshPlugin(() -> extensions.getObject().contributions(ModelSource.POINT), List.of(pcb));
    }

    @Bean
    public ModelDocuments cadModelDocuments(CadCatalogue catalogue, CadLanguage language, DocumentFiles files,
                                            CadAuthority authority, CadPlaces places) {
        return new ModelDocuments(catalogue, language, files, authority, places);
    }

    @Bean
    public ModelMeshService cadModelMeshService(ModelDocuments models, Extensions extensions, TableSource tables) {
        return new ModelMeshService(models, extensions, tables);
    }

    @Bean
    public ModelMeshController cadModelMeshController(ModelMeshService meshes, FeaturedDesigns featured) {
        CadAccessRules.requireDecidedInside(ModelMeshController.class);
        return new ModelMeshController(meshes, featured);
    }
}
