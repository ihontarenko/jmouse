package org.jmouse.testing_ground.binder.dto;

import java.util.List;

public class User {

    private String name;
    private List<Address>   address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Address> getAddress() {
        return address;
    }

    public void setAddress(List<Address> address) {
        this.address = address;
    }
}
