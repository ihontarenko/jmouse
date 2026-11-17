package org.jmouse.schemas;

import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * A schema written from a type, and what could not be said in it: a record or a component its Javadoc does not describe, a type
 * no schema says. A schema with anything undescribed is still written; whoever ships it decides whether that is a mismatch.
 *
 * @param schema      the schema, draft 2020-12
 * @param undescribed what could not be said, each in words naming where
 */
public record WrittenSchema(ObjectNode schema, List<String> undescribed) {

    public WrittenSchema {
        undescribed = List.copyOf(undescribed);
    }
}
