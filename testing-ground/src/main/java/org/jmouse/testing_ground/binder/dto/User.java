package org.jmouse.testing_ground.binder.dto;

import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.core.bind.BindRequired;

import java.util.*;

@BeanFactories(name = "userDTO")
public class User {

    private String name;
    private List<Address>   address;
    private Set<Book>       books;
    private UserStatus status;

    @BindRequired
    public String getName() {
        return name;
    }

    @BindRequired
    public void setName(@Qualifier("uname") @AggregatedBeans String name) {
        this.name = name;
    }

    public void setCopy(Locale.LanguageRange range) {

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

    public UserStatus getStatus() {
        return status;
    }

    // @BindRequired("Обовязкове занчення блять!!!")
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public void setBatchData(List<Map<String, HashMap<String, List<User>>>> batchData) {

    }

}
