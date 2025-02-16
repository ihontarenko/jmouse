package org.jmouse.testing_ground.binder.dto;

import org.jmouse.core.bind.BindParam;

public class Address {

    private String street;
    private String city;

    public Address(@BindParam("street") String street, String city) {
        this.street = street;
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
