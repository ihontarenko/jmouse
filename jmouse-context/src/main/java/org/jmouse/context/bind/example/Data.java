package org.jmouse.context.bind.example;

import org.jmouse.context.bind.PropertyPath;

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
