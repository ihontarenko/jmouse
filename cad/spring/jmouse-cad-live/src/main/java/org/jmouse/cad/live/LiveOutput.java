package org.jmouse.cad.live;

import java.nio.ByteBuffer;

/** Where a running circuit's messages go: the control messages as JSON text, the sample frames as binary. */
interface LiveOutput {

    void text(String message);

    void binary(ByteBuffer frame);
}
