package org.jmouse.jdbc.tx;

import java.sql.Connection;

public final class BoundConnectionScope implements AutoCloseable {

    private final ConnectionBinding binding;
    private final Connection        previous;
    private       boolean           closed;

    public BoundConnectionScope(ConnectionBinding binding, Connection connection) {
        this.binding = binding;
        this.previous = binding.currentConnection();
        binding.bind(connection);
    }

    @Override
    public void close() {
        if (!closed) {
            if (previous != null) {
                binding.bind(previous);
            } else {
                binding.unbind();
            }
            closed = true;
        }
    }
}