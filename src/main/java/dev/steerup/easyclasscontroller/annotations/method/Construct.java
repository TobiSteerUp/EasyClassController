/*
 * Copyright (c) 2021.
 *
 * @author Tobias Cremer
 * @project EasyClassController
 */

package dev.steerup.easyclasscontroller.annotations.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Txb1 at 07.04.2021
 * @project Controller
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Construct {
    Priority value() default Priority.NORMAL;

    enum Priority {
        HIGHEST, HIGH, NORMAL, LOW, LOWEST
    }
}