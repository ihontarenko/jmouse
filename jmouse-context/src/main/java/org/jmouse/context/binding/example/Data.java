package org.jmouse.context.binding.example;

import org.jmouse.context.binding.PropertyPath;

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
