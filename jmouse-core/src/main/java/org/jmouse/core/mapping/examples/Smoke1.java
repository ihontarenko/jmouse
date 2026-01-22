package org.jmouse.core.mapping.examples;

import java.util.Date;

public class Smoke1 {

    public static void main(String... arguments) {
        // тут будемо тестити
    }

    record UserDTO(
            String user, String password,
            UserDetailsDTO details
    ) {

    }

    record UserDetailsDTO(DateOfBirth dateOfBirth, Status status) {

    }

    enum Status {
        BLOCKED, UNBLOCKED
    }

    class DateOfBirth {

        private Date dateOfBirth;

        public Date getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(Date dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
    }

}
