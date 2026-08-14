package org.jmouse.ai;

import java.util.List;

/**
 * What one feature lets a caller do.
 *
 * <p>Declared by the feature that owns the capability, never by whatever collects them. That
 * direction is the whole arrangement: a feature can offer an action without importing a tool server,
 * and the server never becomes the one place that knows every feature's name. A ninth capability costs
 * a bean where the capability lives, and nothing central is edited.
 *
 * <p><strong>A namespace may be contributed by more than one definition, and that is not a bug to
 * guard against.</strong> It is how a vocabulary that spans two features stays one vocabulary: where
 * a place and the things filed in it belong to different parts of a system, a caller should still see
 * one {@code storage} tool rather than two halves it has to reconcile. Only
 * {@link ToolAction#publishedName()} must be unique, and {@link ToolCatalog} checks exactly that.
 */
public interface ToolDefinition {

    /** The namespace, in the domain's own vocabulary — lower case, and whichever of singular or plural the domain says. */
    String toolName();

    /** Every action this feature contributes to that namespace. */
    List<ToolAction> actions();
}
