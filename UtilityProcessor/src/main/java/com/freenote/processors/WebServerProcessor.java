package com.freenote.processors;

import com.freenote.annotations.WebSocketServer;
import com.freenote.exceptions.URIHandlerException;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
public class WebServerProcessor extends AbstractProcessor {
    private Messager messager;
    private final List<String> expectedParams = List.of(
            InputStream.class.getCanonicalName(),
            OutputStream.class.getCanonicalName()
    );

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            var elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(WebSocketServer.class);
            for (var element : elementsAnnotatedWith) {
                if (!(element instanceof TypeElement)) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "WebSocketServer annotation can only be applied to classes.");
                    return false;
                }
                WebSocketServer annotation = element.getAnnotation(WebSocketServer.class);
                String path = annotation.value();
                if (path.isEmpty()) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "WebSocketServer path cannot be empty.", element);
                    return false;
                }
                var allTypeElements = element.getEnclosedElements();
                for (var enclosedElement : allTypeElements) {
                    if (enclosedElement.getKind() == ElementKind.METHOD) {
                        if (!enclosedElement.getModifiers().contains(Modifier.PUBLIC)) {
                            messager.printMessage(Diagnostic.Kind.ERROR, "WebSocketServer methods must be public.", enclosedElement);
                            return false;
                        }
                        ExecutableElement method = (ExecutableElement) enclosedElement;
                        List<? extends VariableElement> params = method.getParameters();
                        checkIncludeRequireMethods(annotation, method, params);
                    }
                }
                messager.printMessage(Diagnostic.Kind.NOTE, "Processing WebSocketServer at path: " + path, element);
            }
            return true;
        } catch (URIHandlerException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage() + e.getPath());
            return false;
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private void checkIncludeRequireMethods(WebSocketServer annotation, ExecutableElement method, List<? extends VariableElement> params) throws URIHandlerException {
        // name
        if (!method.getSimpleName().contentEquals("handle")) {
            throw new URIHandlerException(annotation.value(), "WebSocketServer method '%s' must be named 'handle'.");
        }

        this.messager.printMessage(Diagnostic.Kind.NOTE, "Checking method: " + method.getSimpleName() + " with return type: " + method.getReturnType().getKind());
        // return type
        if (!method.getReturnType().getKind().toString().equals(Boolean.class.getSimpleName().toUpperCase())) {
            throw new URIHandlerException(annotation.value(), String.format("WebSocketServer method '%s' must return boolean.", method.getSimpleName().toString()));
        }

        if (params.size() != 2) {
            throw new URIHandlerException(annotation.value(), "WebSocketServer method '%s' must have exactly two parameters: InputStream and OutputStream.");
        }

        for (int i = 0; i < params.size(); i++) {
            if (!params.get(i).asType().toString().equals(expectedParams.get(i))) {
                throw new URIHandlerException(annotation.value(), "WebSocketServer method '%s' must have exactly two parameters: InputStream and OutputStream.");
            }
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(WebSocketServer.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}