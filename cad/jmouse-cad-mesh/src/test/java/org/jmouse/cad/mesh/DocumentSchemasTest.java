package org.jmouse.cad.mesh;

import org.jmouse.cad.document.DocumentSchemas;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Each kind of document this module brings keeps the schema its model says now, every type and field of it described. */
class DocumentSchemasTest {

    @Test
    void keepsTheSchemaOfEachKindsModel() {
        assertEquals(List.of(), DocumentSchemas.mismatches(new ModelModule(), Path.of("src/main/resources"), DocumentSchemas.Writing.asked()));
    }
}
