package com.freenote.app.server.annotations;

import java.lang.annotation.*;

@Target({java.lang.annotation.ElementType.TYPE, ElementType.CONSTRUCTOR})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FullToolAnnotation {

}
