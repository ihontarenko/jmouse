package org.jmouse.testing_ground.binder.dto;

import java.util.List;
import java.util.Set;

public class User {

    private String name;
    private List<Address>   address;
    private Set<Book>       books;

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

    public Set<Book> getBooks() {
        return books;
    }

    public void setBooks(Set<Book> books) {
        this.books = books;
    }
}
