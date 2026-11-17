package org.jmouse.cad.plugin.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A model brought in as a file is coded in the namespace of the folder it stands in, as the library it came from codes it. */
class ModelFilesImportTest {

    @Test
    void aModelInALibraryFolderIsCodedAsThatLibraryCodesIt() {
        assertEquals("RESISTOR-SMD:R-0805-2012METRIC:M3D",
                ModelFilesImport.codeOf("libraries/Resistor_SMD.3dshapes/R_0805_2012Metric.step", "R_0805_2012Metric"));
    }

    @Test
    void aModelInAPlainFolderIsCodedInThatFolder() {
        assertEquals("HEADERS:PIN-HEADER-1X2:M3D", ModelFilesImport.codeOf("headers/pin header 1x2.glb", "pin header 1x2"));
    }

    @Test
    void aModelInNoFolderIsCodedInTheFormatsOwnNamespace() {
        assertEquals("MODEL:TO-220:M3D", ModelFilesImport.codeOf("TO-220.stl", "TO-220"));
    }
}
