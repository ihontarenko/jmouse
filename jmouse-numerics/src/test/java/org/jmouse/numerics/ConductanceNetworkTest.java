package org.jmouse.numerics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConductanceNetworkTest {

    private static final ConductanceNetwork.Stopping STOPPING = new ConductanceNetwork.Stopping(1e-12, 4, 100);

    @Test
    void resistorsInSeriesAddUp() {
        ConductanceNetwork network = new ConductanceNetwork(3);
        network.connect(0, 1, 1.0);
        network.connect(1, 2, 0.5);
        double[] potentials = network.potentials(0, 2, STOPPING);
        assertEquals(3.0, potentials[0], 1e-9);
        assertEquals(2.0, potentials[1], 1e-9);
        assertEquals(0.0, potentials[2], 1e-12);
    }

    @Test
    void resistorsSideBySideShareTheCurrent() {
        ConductanceNetwork network = new ConductanceNetwork(2);
        network.connect(0, 1, 0.5);
        network.connect(1, 0, 0.5);
        assertEquals(1.0, network.potentials(0, 1, STOPPING)[0], 1e-9);
    }

    @Test
    void aBalancedBridgeCarriesNothingAcrossItsMiddle() {
        // A Wheatstone bridge of equal arms: the middle conductance changes nothing, the resistance is one arm's.
        ConductanceNetwork network = new ConductanceNetwork(4);
        network.connect(0, 1, 1);
        network.connect(0, 2, 1);
        network.connect(1, 3, 1);
        network.connect(2, 3, 1);
        network.connect(1, 2, 7);
        double[] potentials = network.potentials(0, 3, STOPPING);
        assertEquals(1.0, potentials[0], 1e-9);
        assertEquals(potentials[1], potentials[2], 1e-9);
    }

    @Test
    void aLongLadderIsSolvedToItsTolerance() {
        int rungs = 2000;
        ConductanceNetwork network = new ConductanceNetwork(rungs);
        for (int node = 1; node < rungs; node++) {
            network.connect(node - 1, node, 10);
        }
        assertEquals((rungs - 1) / 10.0, network.potentials(0, rungs - 1, STOPPING)[0], 1e-6);
    }
}
