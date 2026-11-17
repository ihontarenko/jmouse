package org.jmouse.schemas;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * ✔️ A value held to a schema: what in it the schema does not allow, each in words naming where — none where it holds. Checked by
 * networknt's validator, draft 2020-12, so a schema written here is proved against the values it claims to describe.
 */
public final class SchemaChecks {

    private static final SchemaRegistry REGISTRY = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    private SchemaChecks() {
    }

    /** What in a value its schema does not allow. */
    public static List<String> problemsOf(ObjectNode schema, JsonNode value) {
        Schema checked = REGISTRY.getSchema(schema);
        return checked.validate(value).stream().map(problem -> problem.getInstanceLocation() + ": " + problem.getMessage()).toList();
    }
}
