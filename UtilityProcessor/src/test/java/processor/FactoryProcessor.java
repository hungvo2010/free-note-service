package processor;

import annotation.Factory;
import com.freenote.exceptions.ProcessingException;
import group.FactoryAnnotatedClass;
import group.FactoryGroupedClasses;
import com.google.auto.service.AutoService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.freenote.utils.LogUtils.error;

@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {

    private static final Logger log = LogManager.getLogger(FactoryProcessor.class);
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private final Map<String, FactoryGroupedClasses> factoryClasses = new LinkedHashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Factory.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            // Scan classes
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {
                log.info("Processing element: {}", annotatedElement.getSimpleName());

                // Check if a class has been annotated with @Factory
                if (annotatedElement.getKind() != ElementKind.CLASS) {
                    throw new ProcessingException(annotatedElement, "Only classes can be annotated with @%s",
                            Factory.class.getSimpleName());
                }

                // We can cast it because we know that IT of ElementKind.CLASS
                TypeElement typeElement = (TypeElement) annotatedElement;

                FactoryAnnotatedClass annotatedClass = new FactoryAnnotatedClass(typeElement);

                checkValidClass(annotatedClass);

                // Everything is fine, so try to add
                FactoryGroupedClasses factoryClass =
                        factoryClasses.get(annotatedClass.getQualifiedFactoryGroupName());
                if (factoryClass == null) {
                    String qualifiedGroupName = annotatedClass.getQualifiedFactoryGroupName();
                    factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
                    factoryClasses.put(qualifiedGroupName, factoryClass);
                }

                // Checks if id is conflicting with another @Factory annotated class with the same id
                factoryClass.add(annotatedClass);
            }

            // Generate code
            for (FactoryGroupedClasses factoryClass : factoryClasses.values()) {
                factoryClass.generateCode(elementUtils, filer);
            }
            factoryClasses.clear();
        } catch (ProcessingException e) {
            error(this.messager, e.getElement(), e.getMessage());
        } catch (IOException e) {
            error(this.messager, null, e.getMessage());
        }

        return true;
    }

    /**
     * Checks if the annotated element observes our rules
     */
    private void checkValidClass(FactoryAnnotatedClass item) throws ProcessingException {

        // Cast to TypeElement, has more types specific methods
        TypeElement classElement = item.getTypeElement();

        if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
            throw new ProcessingException(classElement, "The class %s annotated with @%s must inherit from %s", classElement.getQualifiedName().toString(), "The class %s is not public.",
                    classElement.getQualifiedName().toString());
        }

        // Check if it's an abstract class
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new ProcessingException(classElement,
                    "The class %s is abstract. You can't annotate abstract classes with @%",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
        }

        TypeElement superClassElement = elementUtils.getTypeElement(item.getQualifiedFactoryGroupName());
        checkImplementInterface(item, superClassElement, classElement);
        checkSubclassOfAnyType(item, classElement);

        if (!checkDefaultConstructor(classElement)) {
            throw new ProcessingException(classElement,
                    "[LATER]The class %s annotated with @%s must inherit from %s",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                    item.getQualifiedFactoryGroupName());
        }
    }

    private boolean checkDefaultConstructor(TypeElement classElement) {
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructorElement = (ExecutableElement) enclosed;
                if (constructorElement.getParameters().isEmpty() && constructorElement.getModifiers()
                        .contains(Modifier.PUBLIC)) {
                    // Found an empty constructor
                    return true;
                }
            }
        }
        return false;
    }

    private void checkImplementInterface(FactoryAnnotatedClass item, TypeElement superClassElement, TypeElement classElement) throws ProcessingException {
        if (superClassElement.getKind() == ElementKind.INTERFACE && !classElement.getInterfaces().contains(superClassElement.asType())) {
            throw new ProcessingException(classElement,
                    "The class %s annotated with @%s must implement the interface %s",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                    item.getQualifiedFactoryGroupName());
        }
    }

    private void checkSubclassOfAnyType(FactoryAnnotatedClass item, TypeElement classElement) throws ProcessingException {
        TypeMirror superClassType = classElement.getSuperclass();
//        this.messager.printMessage(Diagnostic.Kind.NOTE, "Checking superclass: " + superClassType.toString());

        if (superClassType.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) superClassType;
            Element element = declaredType.asElement();
            if (element.getKind() == ElementKind.CLASS && !element.toString().equals(Object.class.getCanonicalName())) {
                throw new ProcessingException(classElement,
                        "[SUBCLASS]The class %s annotated with @%s must inherit from %s",
                        classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                        item.getQualifiedFactoryGroupName());
            }
        }
    }
}
