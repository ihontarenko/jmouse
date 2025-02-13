package org.jmouse.testing_ground.binder;

import org.jmouse.core.bind.PropertyName;

public class Data {

    private int number;

    public int getNumber() {
        return number;
    }

    @PropertyName("random")
    public void setNumber(int number) {
        this.number = number;
    }
}
