package org.jmouse.testing_ground.binder.dto;

import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.core.bind.BindRequired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@BeanFactories
@Configuration(name = "userDTO")
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
