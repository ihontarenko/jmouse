package org.jmouse.example;

import org.jmouse.core.bind.PropertyPath;

public class Data {

    private int number;

    public int getNumber() {
        return number;
    }

    @PropertyPath("random")
    public void setNumber(int number) {
        this.number = number;
    }
}
