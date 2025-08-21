package com.freenote.utils;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class LogUtils {
    private LogUtils() {
    }

    public static void error(Messager messager, Element e, String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    public static void info(Messager messager, Element e, String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg, e);
    }
}
