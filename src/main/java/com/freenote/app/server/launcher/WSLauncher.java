package com.freenote.app.server.launcher;

import com.freenote.annotations.Singleton;
import com.freenote.app.server.annotations.URIHandleAnnotation;
import com.freenote.exceptions.TwoSingletonException;
import com.freenote.app.server.handler.URIHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class WSLauncher {
    private static final Logger log = LogManager.getLogger(WSLauncher.class);
    public static final Map<String, Class<URIHandler>> ALL_URI_HANDLERS = new HashMap<>();

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    private static void extractMethod() throws IOException, ClassNotFoundException {
        String packageName = "com.freenote.app.server.example"; // change to your base package
        List<Class<?>> classes = getClasses(packageName);

        for (Class<?> clazz : classes) {
            Annotation[] annotations = clazz.getAnnotations();
            var methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                Annotation[] methodAnnotations = method.getAnnotations();
                if (methodAnnotations.length > 0) {
                    System.out.println("Method: " + method.getName());
                    System.out.println("Return Type: " + method.getReturnType().getName());
                    System.out.println("Parameter Count: " + method.getParameterCount());
                    for (Annotation ann : methodAnnotations) {
                        System.out.println("  -> " + ann.annotationType().getName());
                    }
                }
            }
            if (annotations.length > 0) {
                System.out.println(clazz.getName());
                for (Annotation ann : annotations) {
                    System.out.println("  -> " + ann.annotationType().getName());
                }
            }
        }
    }

    public static void launch(String[] args) throws IOException, ClassNotFoundException {
//        checkSingletonBean();
//        injectAllDependencies();
    }

    private static void injectAllDependencies() throws IOException, ClassNotFoundException {
        var mainPackage = getMainPackage();
        var classesWithAnnotation = getClasses(mainPackage);
        for (Class<?> clazz : classesWithAnnotation) {
            injectURIHandlerClass(clazz);
        }
    }

    private static void injectURIHandlerClass(Class<?> clazz) {
        try {
            if (!clazz.isAnnotationPresent(URIHandleAnnotation.class)) {
                return;
            }
            Method method = clazz.getMethod("handle", InputStream.class, OutputStream.class);
            if (method == null) {
                log.error(String.format("Class %s does not have a handle method with the correct signature.", clazz.getName()));
                return;
            }
            ALL_URI_HANDLERS.put(clazz.getAnnotation(URIHandleAnnotation.class).path(), (Class<URIHandler>) clazz);
        } catch (NoSuchMethodException e) {
            System.err.println("Class " + clazz.getName() + " does not have a handle method with the correct signature.");
            e.printStackTrace();
        } catch (SecurityException e) {
            log.info("Security exception while accessing method in class " + clazz.getName());
            e.printStackTrace();
        }

    }

    private static String getMainPackage() {
        return null;
    }

    private static void checkSingletonBean() throws IOException, ClassNotFoundException {
        List<Class<?>> classes = getClasses("com.freenote.app.server");
        var singletonClasses = new ArrayList<Class<?>>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Singleton.class)) {
                singletonClasses.add(clazz);
            }
        }
        if (singletonClasses.size() > 1) {
            throw new TwoSingletonException("There are multiple classes annotated with @Singleton in the package.");
        }
    }

    private static List<Class<?>> getClasses(String packageName) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files == null) return classes;
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
        return classes;
    }
}
