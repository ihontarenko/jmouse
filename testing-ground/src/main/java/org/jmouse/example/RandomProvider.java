package org.jmouse.example;

import java.util.Random;

public class RandomProvider {

    public int getRandom() {
        return new Random().nextInt();
    }

}
