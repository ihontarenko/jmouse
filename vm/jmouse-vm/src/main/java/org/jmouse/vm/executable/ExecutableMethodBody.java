package org.jmouse.vm.executable;

import org.jmouse.vm.executable.node.ExecutableNode;
import org.jmouse.vm.program.ExecutableMethodKey;

/** Shared frontend/backend seam; operation kinds inside either representation remain extensible. */
public sealed interface ExecutableMethodBody permits StructuredMethodBody, ControlFlowMethodBody {

    ExecutableMethodKey owner();

    ExecutableNode root();
}
