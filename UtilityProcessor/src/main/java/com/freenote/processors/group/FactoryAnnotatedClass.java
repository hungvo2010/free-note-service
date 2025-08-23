package com.freenote.processors.group;


import com.freenote.annotations.Factory;
import com.freenote.exceptions.ProcessingException;
import lombok.Getter;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

public class FactoryAnnotatedClass {

    private final TypeElement annotatedClassElement;
    private String qualifiedGroupClassName;
    @Getter
    private String simpleFactoryGroupName;
    @Getter
    private final String id;

    /**
     * @throws ProcessingException if id() from annotation is null
     */
    public FactoryAnnotatedClass(TypeElement classElement) throws ProcessingException {
        this.annotatedClassElement = classElement;
        Factory annotation = classElement.getAnnotation(Factory.class);
        id = annotation.id();

        if (id == null || id.isEmpty()) {
            throw new ProcessingException(classElement,
                    "id() in @%s for class %s is null or empty! that's not allowed",
                    Factory.class.getSimpleName(), classElement.getQualifiedName().toString());
        }

        // Get the full QualifiedTypeName
        try {
            Class<?> clazz = annotation.type();
            qualifiedGroupClassName = clazz.getCanonicalName();
            simpleFactoryGroupName = clazz.getSimpleName();
        } catch (MirroredTypeException mte) {
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            qualifiedGroupClassName = classTypeElement.getQualifiedName().toString();
            simpleFactoryGroupName = classTypeElement.getSimpleName().toString();
        }
    }

    public String getQualifiedFactoryGroupName() {
        return qualifiedGroupClassName;
    }

    public TypeElement getTypeElement() {
        return annotatedClassElement;
    }
}