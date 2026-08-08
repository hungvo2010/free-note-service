package com.freenote.app.test.TestIteratorModified;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class PersonTest implements Serializable {
    private static final long serialVersionUID = -1249125912102581251L;
    static String country = "ITALY";
    private int age;
    private String name;
    transient int height;

    // getters and setters

}
