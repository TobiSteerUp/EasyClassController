/*
 * Copyright (c) 2021.
 *
 * @author Tobias Cremer
 * @project EasyClassController
 */

package dev.steerup.easyclasscontroller.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {

    public static void setFieldValue(Field field, Object instance, Object value) {
        field.setAccessible(true);
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void invokeMethod(Method method, Object instance, Object... args) {
        method.setAccessible(true);
        try {
            method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public <T> T getFieldValue(Class<T> clazz, Field field, Object instance) {
        Object fieldValue = getFieldValue(field, instance);
        return clazz.cast(fieldValue);
    }

    public static Object getFieldValue(Field field, Object instance) {
        field.setAccessible(true);
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}