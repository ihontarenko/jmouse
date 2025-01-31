package org.jmouse.context.binding.test;

import java.util.List;
import java.util.Map;

public class Customer {

    private     String          name;
    private int                 age;
    private List<Integer>       access = List.of(111, 222, 333);
    private Map<String, String> groups = Map.of("default", "Default Group");

    private User parent;

    public Customer(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public Customer() {
    }

    public User getUser() {
        return parent;
    }

    public void setUser(User user) {
        this.parent = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<Integer> getAccess() {
        return access;
    }

    public void setAccess(List<Integer> access) {
        this.access = access;
    }

    public Map<String, String> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, String> groups) {
        this.groups = groups;
    }

    @Override
    public String toString() {
        return "Kastamer{name='%s', age=%d}".formatted(name, age);
    }

}
