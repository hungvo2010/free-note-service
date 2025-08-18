package com.freenote.app.server.annotationProcessor;

import lombok.Getter;

import javax.lang.model.element.Element;

@Getter
public class ProcessingException extends Exception {
    private final Element element;

    public ProcessingException(Element classElement, String s, String simpleName, String string) {
        super(String.format(s, simpleName, classElement.getSimpleName(), string));
        this.element = classElement;
    }

    public ProcessingException(Element annotatedElement, String s1, String string, String s, String simpleName) {
        super(String.format(s1, string, simpleName, annotatedElement.getSimpleName()));
        this.element = annotatedElement;
    }

    public ProcessingException(Element annotatedElement, String s, String simpleName) {
        super(String.format(s, simpleName, annotatedElement.getSimpleName()));
        this.element = annotatedElement;
    }

}
