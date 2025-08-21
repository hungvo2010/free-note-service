package com.freenote.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.CONSTRUCTOR})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FullToolAnnotation {

}
