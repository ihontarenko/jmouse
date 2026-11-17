package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.DocumentSchemas;
import org.jmouse.schemas.SchemaChecks;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every shipped document's model, written as the engine answers it — nulls kept —, holds to the schema its kind keeps: what a
 * schema claims of a model is proved on every document the library holds, not only on the types.
 */
class DocumentModelsHoldToSchemasTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void everyShippedDocumentsModelHoldsToItsKindsSchema() {
        CadLibrary library = CadLibrary.library();
        CadLanguage language = CadLanguage.shipped();
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (String slug : library.slugs()) {
            DocumentNode document = library.find(slug).orElseThrow();
            Optional<Object> model = language.modelOf(document);
            if (model.isEmpty()) {
                continue;
            }
            Optional<ObjectNode> schema = DocumentSchemas.kept(document.kind());
            if (schema.isEmpty()) {
                problems.add(slug + ": its kind " + document.kind().word() + " keeps no schema");
                continue;
            }
            SchemaChecks.problemsOf(schema.get(), JSON.valueToTree(model.get())).stream()
                        .forEach(problem -> problems.add(slug + " " + problem));
            checked++;
        }
        assertTrue(checked > 100, "only " + checked + " documents were checked");
        assertEquals(List.of(), problems);
    }
}
