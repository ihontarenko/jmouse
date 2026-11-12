package org.jmouse.cad.plugin.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A model brought in as a file is coded in the namespace of the folder it stands in, as the library it came from codes it. */
class ModelFilesImportTest {

    @Test
    void aModelInALibraryFolderIsCodedAsThatLibraryCodesIt() {
        assertEquals("MD.RESISTOR-SMD:R-0805-2012METRIC",
                ModelFilesImport.codeOf("libraries/Resistor_SMD.3dshapes/R_0805_2012Metric.step", "R_0805_2012Metric"));
    }

    @Test
    void aModelInAPlainFolderIsCodedInThatFolder() {
        assertEquals("MD.HEADERS:PIN-HEADER-1X2", ModelFilesImport.codeOf("headers/pin header 1x2.glb", "pin header 1x2"));
    }

    @Test
    void aModelInNoFolderIsCodedInTheFormatsOwnNamespace() {
        assertEquals("MD.MODEL:TO-220", ModelFilesImport.codeOf("TO-220.stl", "TO-220"));
    }
}
