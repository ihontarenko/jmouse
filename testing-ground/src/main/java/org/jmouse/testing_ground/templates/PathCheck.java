package org.jmouse.testing_ground.templates;

import org.jmouse.core.access.PropertyPath;

public class PathCheck {

    public static void main(String[] args) {
        PropertyPath path = PropertyPath.forPath("books");

        for (CharSequence entry : path.entries()) {
            System.out.println( entry);
        }
        System.out.println("----");
        System.out.println(path.sub(1));
        System.out.println(path.sup(1));

        System.out.println(path.entries().slice(0, 2));
        System.out.println(path.entries().slice(2, 4));

        System.out.println(path.entries().limit(1));
        System.out.println(path.tail());
        System.out.println(path.head());


    }

}
