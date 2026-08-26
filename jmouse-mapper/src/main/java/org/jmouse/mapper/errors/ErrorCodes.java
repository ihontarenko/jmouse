package org.jmouse.mapper.errors;

/**
 * Stable machine-readable error codes for mapping engine.
 *
 * <p>Taxonomy (namespace.prefix):</p>
 * <ul>
 *   <li>strategy.*    - strategy selection/build problems</li>
 *   <li>bean.*        - bean mapping failures</li>
 *   <li>record.*      - record mapping failures</li>
 *   <li>map.*         - map mapping failures</li>
 *   <li>collection.*  - collections/arrays mapping failures</li>
 *   <li>scalar.*      - scalar conversion failures</li>
 *   <li>binding.*     - binding specification problems</li>
 * </ul>
 */
public final class ErrorCodes {

    // strategy.*
    public static final String STRATEGY_EXECUTION_FAILED  = "strategy.execution_failed";
    public static final String STRATEGY_NO_CONTRIBUTOR    = "strategy.no_contributor";
    public static final String STRATEGY_INCOMPATIBLE_TYPE = "strategy.incompatible_type";

    // bean.*
    public static final String BEAN_INSTANTIATION_FAILED  = "bean.instantiation_failed";
    public static final String BEAN_PROPERTY_ADAPT_FAILED = "bean.property_adapt_failed";
    public static final String BEAN_PROPERTY_WRITE_FAILED = "bean.property_write_failed";

    // record.*
    public static final String RECORD_TARGET_NOT_RECORD        = "record.target_not_record";
    public static final String RECORD_COMPONENT_ADAPT_FAILED   = "record.component_adapt_failed";
    public static final String RECORD_INSTANTIATION_FAILED     = "record.instantiation_failed";

    // map.*
    public static final String MAP_ENTRY_ADAPT_FAILED = "map.entry_adapt_failed";
    public static final String MAP_KEY_NOT_STRING     = "map.key_not_string";

    // mapping.*
    public static final String MAPPING_DEPTH_EXCEEDS       = "mapping.depth_exceeds";

    // ⚠️ A family, not two constants. ErrorsPolicy matches by PREFIX, so an application governs every
    // refusal at once with onPrefix("refuse.", …) — deciding for itself whether a declaration that
    // stops a mapping throws, warns, or is ignored while a data problem is being cleaned up.
    public static final String REFUSE_SOURCE               = "refuse.source";
    public static final String REFUSE_TARGET               = "refuse.target";
    public static final String MAPPING_REFERENCE_CYCLE     = "mapping.reference_cycle";

    // collection.*
    public static final String COLLECTION_SIZE_EXCEEDS     = "collection.size_exceeds";

    // array.*
    public static final String ARRAY_SIZE_EXCEEDS     = "array.size_exceeds";

    // scalar.*
    public static final String SCALAR_CONVERSION_FAILED = "scalar.conversion_failed";

    // binding.*
    public static final String BINDING_SOURCE_TYPE_MISMATCH         = "binding.source_type_mismatch";
    public static final String BINDING_COMPUTE_SOURCE_TYPE_MISMATCH = "binding.compute_source_type_mismatch";

    private ErrorCodes() {
    }
}
