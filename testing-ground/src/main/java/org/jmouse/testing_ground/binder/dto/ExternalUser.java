package org.jmouse.testing_ground.binder.dto;

import org.jmouse.beans.annotation.Factories;
import org.jmouse.core.bind.BindRequired;

@Factories(name = "userDTO")
public class ExternalUser {

    private String name;
    private String mainAddress;

    public String getName() {
        return name;
    }

    @BindRequired
    public void setName(String name) {
        this.name = name;
    }

    public String getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(String mainAddress) {
        this.mainAddress = mainAddress;
    }
}
