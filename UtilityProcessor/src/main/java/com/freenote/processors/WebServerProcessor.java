package com.freenote.processors;

import com.freenote.annotations.WebSocketEndpoint;
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

import static com.freenote.utils.LogUtils.error;

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
            var elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(WebSocketEndpoint.class);
            for (var element : elementsAnnotatedWith) {
                if (!(element instanceof TypeElement)) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "WebSocketServer annotation can only be applied to classes.");
                    return true;
                }
                WebSocketEndpoint annotation = element.getAnnotation(WebSocketEndpoint.class);
                String path = annotation.value();
                if (path.isEmpty()) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "WebSocketServer path cannot be empty.", element);
                    return true;
                }
                var allTypeElements = element.getEnclosedElements();
                var valid = allTypeElements.stream().filter(e -> e.getKind() == ElementKind.METHOD)
                        .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
                        .filter(this::isMatchSignature)
                        .count() == 1;
                if (!valid) {
                    error(messager, element, "WebSocketServer class must have exactly one public method named 'handle' with the correct signature.");
                }
                return true;
            }
            return true;
        } catch (Exception e) {
            error(messager, null, "An unexpected error occurred: " + e.getMessage());
            return true;
        }
    }

    private boolean isMatchSignature(Element element) {
        try {
            WebSocketEndpoint annotation = element.getAnnotation(WebSocketEndpoint.class);
            ExecutableElement method = (ExecutableElement) element;
            List<? extends VariableElement> params = method.getParameters();
            checkIncludeRequireMethods(annotation, method, params);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkIncludeRequireMethods(WebSocketEndpoint annotation, ExecutableElement method, List<? extends VariableElement> params) throws URIHandlerException {
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Checking method: " + method.getSimpleName() + " with return type: " + method.getReturnType().getKind());
        // return type
//        if (!method.getReturnType().getKind().toString().equals(Boolean.class.getSimpleName().toUpperCase())) {
//            throw new URIHandlerException(annotation.value(), String.format("WebSocketServer method '%s' must return boolean.", method.getSimpleName().toString()));
//        }

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
        return Set.of(WebSocketEndpoint.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}