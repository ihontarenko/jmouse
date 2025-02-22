package org.jmouse.testing_ground.introspector;

public class ClientUser {

    private final String name;
    private final String lastName;
    private final int    age;

    public ClientUser(String name, String lastName, int age) {
        this.name = name;
        this.lastName = lastName;
        this.age = age;
    }

    public ClientUser(String name, String lastName) {
        this(name, lastName, 0);
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public int getAge() {
        return age;
    }

}
