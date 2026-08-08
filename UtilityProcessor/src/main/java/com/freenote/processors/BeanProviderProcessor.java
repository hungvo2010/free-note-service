package com.freenote.processors;

import com.freenote.annotations.WebSocketEndpoint;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.freenote.utils.LogUtils.error;
import static com.freenote.utils.LogUtils.info;

@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class BeanProviderProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        List<AnnotatedClass> annotatedClasses = new ArrayList<>();

        // Collect all annotated classes
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(WebSocketEndpoint.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                error(this.messager, annotatedElement,
                        "@URIHandlerImplementation can only be applied to classes");
                continue;
            }

            TypeElement classElement = (TypeElement) annotatedElement;
            WebSocketEndpoint annotation = classElement.getAnnotation(WebSocketEndpoint.class);

            String className = classElement.getSimpleName().toString();
            String qualifiedName = classElement.getQualifiedName().toString();
            String value = annotation.value();

            info(this.messager, classElement,
                    "Found @WebSocketEndpoint on " + qualifiedName + " with value: " + value);

            annotatedClasses.add(new AnnotatedClass(className, qualifiedName, value));

//            info(this.messager, classElement,
//                    "Processing @URIHandlerImplementation on " + qualifiedName);
        }

        // Generate the registry class
        if (!annotatedClasses.isEmpty()) {
            try {
                generateRegistryClass(annotatedClasses);
            } catch (IOException e) {
                error(this.messager, null, "Failed to generate registry class: " + e.getMessage());
            }
        }

        return true;
    }

    private void generateRegistryClass(List<AnnotatedClass> annotatedClasses) throws IOException {
        String packageName = "generated";
        String className = "URIHandlerRegistry";

        JavaFileObject builderFile = filer.createSourceFile(packageName + "." + className);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            // Package declaration
            out.println("package " + packageName + ";");
            out.println();

            // Imports
            out.println("import java.util.Map;");
            out.println("import java.util.HashMap;");
            out.println("import java.util.List;");
            out.println("import java.util.Set;");
            out.println("import java.util.ArrayList;");
            out.println("import java.util.Collections;");

            // Import all annotated classes
            for (AnnotatedClass annotatedClass : annotatedClasses) {
                out.println("import " + annotatedClass.qualifiedName + ";");
            }
            out.println();

            // Class declaration
            out.println("/**");
            out.println(" * Generated class containing instances of all @URIHandlerImplementation annotated classes");
            out.println(" * This class is automatically generated - do not modify manually");
            out.println(" */");
            out.println("public final class " + className + " {");
            out.println();

            // Private constructor
            out.println("    private " + className + "() {");
            out.println("        // Utility class - prevent instantiation");
            out.println("    }");
            out.println();

            // Static instances map
            out.println("    private static final Map<String, Object> INSTANCES = new HashMap<>();");
            out.println("    private static final Map<String, Object> URI_VALUES = new HashMap<>();");
            out.println("    private static final List<Object> ALL_INSTANCES = new ArrayList<>();");
            out.println();

            // Static initializer block
            out.println("    static {");
            out.println("        initializeInstances();");
            out.println("    }");
            out.println();

            // Initialize instances method
            out.println("    private static void initializeInstances() {");
            out.println("        try {");

            for (AnnotatedClass annotatedClass : annotatedClasses) {
                String instanceName = annotatedClass.className.toLowerCase() + "Instance";
                out.println("            " + annotatedClass.className + " " + instanceName + " = new " + annotatedClass.className + "();");
                out.println("            ALL_INSTANCES.add(" + instanceName + ");");
                out.println("            URI_VALUES.put(\"" + annotatedClass.uriValue + "\", " + instanceName + ");");
                out.println("            INSTANCES.put(\"" + annotatedClass.className + "\", " + instanceName + ");");
            }

            out.println("        } catch (Exception e) {");
            out.println("            throw new RuntimeException(\"Failed to initialize URI handler instances\", e);");
            out.println("        }");
            out.println("    }");
            out.println();

            // Getter methods
            out.println("    /**");
            out.println("     * Get instance by class name");
            out.println("     */");
            out.println("    public static Object getInstance(String className) {");
            out.println("        return INSTANCES.get(className);");
            out.println("    }");
            out.println();

            out.println("    /**");
            out.println("     * Get typed instance by class");
            out.println("     */");
            out.println("    @SuppressWarnings(\"unchecked\")");
            out.println("    public static <T> T getInstance(Class<T> clazz) {");
            out.println("        return (T) INSTANCES.get(clazz.getSimpleName());");
            out.println("    }");
            out.println();

            out.println("    /**");
            out.println("     * Get URI value for a class");
            out.println("     */");
            out.println("    public static String getURIValue(String className) {");
            out.println("        Object instance = INSTANCES.get(className);");
            out.println("        if (instance != null) {");
            out.println("            for (Map.Entry<String, Object> entry : URI_VALUES.entrySet()) {");
            out.println("                if (instance.equals(entry.getValue())) {");
            out.println("                    return entry.getKey();");
            out.println("                }");
            out.println("            }");
            out.println("        }");
            out.println("        return null;");
            out.println("    }");
            out.println();

            out.println("    /**");
            out.println("     * Get all instances");
            out.println("     */");
            out.println("    public static List<Object> getAllInstances() {");
            out.println("        return Collections.unmodifiableList(ALL_INSTANCES);");
            out.println("    }");
            out.println();

            out.println("    /**");
            out.println("     * Get all class names");
            out.println("     */");
            out.println("    public static Set<String> getAllClassNames() {");
            out.println("        return Collections.unmodifiableSet(INSTANCES.keySet());");
            out.println("    }");
            out.println();

            out.println("    /**");
            out.println("     * Get instance by URI value");
            out.println("     */");
            out.println("    public static Object getInstanceByURI(String uriValue) {");
            out.println("        return URI_VALUES.get(uriValue);");
            out.println("    }");
            out.println();

            // Individual getter methods for each class
            for (AnnotatedClass annotatedClass : annotatedClasses) {
                out.println("    /**");
                out.println("     * Get " + annotatedClass.className + " instance");
                out.println("     * URI: " + annotatedClass.uriValue);
                out.println("     */");
                out.println("    public static " + annotatedClass.className + " get" + annotatedClass.className + "() {");
                out.println("        return (" + annotatedClass.className + ") INSTANCES.get(\"" + annotatedClass.className + "\");");
                out.println("    }");
                out.println();
            }

            // Close class
            out.println("}");
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(WebSocketEndpoint.class.getCanonicalName());
    }

    private static class AnnotatedClass {
        final String className;
        final String qualifiedName;
        final String uriValue;

        AnnotatedClass(String className, String qualifiedName, String uriValue) {
            this.className = className;
            this.qualifiedName = qualifiedName;
            this.uriValue = uriValue;
        }
    }
}
