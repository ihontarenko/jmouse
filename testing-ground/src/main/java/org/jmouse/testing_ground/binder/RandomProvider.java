package org.jmouse.testing_ground.binder;

import java.util.Random;

public class RandomProvider {

    public int getRandom() {
        return new Random().nextInt();
    }

}
