package org.jmouse.core.context.mutable;

import org.jmouse.core.context.ProcessingControl;

/**
 * 🛠 Mutable processing control.
 *
 * <p>
 * Allows active mutation of the processing state,
 * typically used by interceptors or handlers
 * to short-circuit further execution.
 * </p>
 */
public interface MutableProcessingControl extends ProcessingControl {

    /**
     * 🛑 Stop further processing.
     */
    void stopProcessing();

}
