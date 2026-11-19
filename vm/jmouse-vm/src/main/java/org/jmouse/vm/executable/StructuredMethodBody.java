package org.jmouse.vm.executable;

import org.jmouse.vm.executable.node.ExecutableBlockNode;
import org.jmouse.vm.program.ExecutableMethodKey;

/**
 * Frozen structured implementation. Construction does not replace semantic validation or linking.
 */
public record StructuredMethodBody(ExecutableMethodKey owner, ExecutableBlockNode root)
        implements ExecutableMethodBody {

    public StructuredMethodBody {
        ExecutableBodyValidation.validate(owner, root);
    }
}
