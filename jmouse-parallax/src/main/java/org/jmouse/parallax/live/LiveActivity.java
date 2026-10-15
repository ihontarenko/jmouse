package org.jmouse.parallax.live;

/**
 * 📡 How many circuits are running live right now, on every socket of this server.
 *
 * <p>The management surface reports it beside the queue and the warm-up, and the live module is what knows it — so
 * the number crosses between the two through this port rather than through a dependency of one module on the other.
 * An installation without live circuits has no implementation, and the count reads as zero.</p>
 */
public interface LiveActivity {

    /**
     * 🔢 The circuits stepping now.
     *
     * @return the count
     */
    int running();
}
