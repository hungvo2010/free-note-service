package com.freenote.app.test.TestIteratorModified;

import java.io.*;

public class DeserializationIDTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Person person = new Person();
        person.setAge(20);
        person.setName("Joe");

        FileOutputStream fileOutputStream
                = new FileOutputStream("yourfile.txt");
        ObjectOutputStream objectOutputStream
                = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(person);
        objectOutputStream.flush();
        objectOutputStream.close();

        FileInputStream fileInputStream
                = new FileInputStream("yourfile.txt");
        ObjectInputStream objectInputStream
                = new ObjectInputStream(fileInputStream);
        Person p2 = (Person) objectInputStream.readObject();
//        byte[] values = objectInputStream.readAllBytes();
//        objectInputStream.close();
        System.out.println(p2.getAge());
        System.out.println(p2.getName());
//        System.out.println(values.length);
    }
}
