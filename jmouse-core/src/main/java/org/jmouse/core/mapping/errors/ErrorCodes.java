package org.jmouse.core.mapping.errors;

/**
 * Stable machine-readable error codes for mapping engine.
 *
 * <p>Taxonomy (namespace.prefix):</p>
 * <ul>
 *   <li>plan.*        - plan selection/build problems</li>
 *   <li>bean.*        - bean mapping failures</li>
 *   <li>record.*      - record mapping failures</li>
 *   <li>map.*         - map mapping failures</li>
 *   <li>collection.*  - collections/arrays mapping failures</li>
 *   <li>scalar.*      - scalar conversion failures</li>
 *   <li>binding.*     - binding specification problems</li>
 *   <li>plugin.*      - plugin pipeline failures</li>
 * </ul>
 */
public final class ErrorCodes {

    // plan.*
    public static final String PLAN_NO_CONTRIBUTOR = "plan.no_contributor";

    // bean.*
    public static final String BEAN_INSTANTIATION_FAILED  = "bean.instantiation_failed";
    public static final String BEAN_PROPERTY_ADAPT_FAILED = "bean.property_adapt_failed";
    public static final String BEAN_PROPERTY_WRITE_FAILED = "bean.property_write_failed";

    // record.*
    public static final String RECORD_TARGET_NOT_RECORD        = "record.target_not_record";
    public static final String RECORD_CANONICAL_CTOR_NOT_FOUND = "record.canonical_ctor_not_found";
    public static final String RECORD_COMPONENT_ADAPT_FAILED   = "record.component_adapt_failed";
    public static final String RECORD_INSTANTIATION_FAILED     = "record.instantiation_failed";

    // map.*
    public static final String MAP_ENTRY_ADAPT_FAILED = "map.entry_adapt_failed";
    public static final String MAP_KEY_NOT_STRING     = "map.key_not_string";

    // scalar.*
    public static final String SCALAR_CONVERSION_FAILED = "scalar.conversion_failed";

    // binding.*
    public static final String BINDING_SOURCE_TYPE_MISMATCH         = "binding.source_type_mismatch";
    public static final String BINDING_COMPUTE_SOURCE_TYPE_MISMATCH = "binding.compute_source_type_mismatch";

    // plugin.*
    public static final String PLUGIN_VALUE_REJECTED = "plugin.value_rejected";

    private ErrorCodes() {
    }
}
