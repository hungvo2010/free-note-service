package com.freenote.app.server.annotations;

import java.lang.annotation.Annotation;

public class TestAnnotation {
    public static void main(String[] args) throws ClassNotFoundException {
        Class <?> clazz = Class.forName("com.freenote.app.server.annotations.Subclass");
        for (Annotation annotation : clazz.getAnnotations()) {
            System.out.println("Annotation: " + annotation.annotationType().getName());
        }
    }
}
