/*
 * Copyright (c) 2021.
 *
 * @author Tobias Cremer
 * @project EasyClassController
 */

package dev.steerup.easyclasscontroller.context.builder;

import dev.steerup.easyclasscontroller.annotations.field.Fill;
import dev.steerup.easyclasscontroller.annotations.field.Load;
import dev.steerup.easyclasscontroller.annotations.field.Provide;
import dev.steerup.easyclasscontroller.annotations.method.Construct;
import dev.steerup.easyclasscontroller.context.Context;
import dev.steerup.easyclasscontroller.context.classes.ClassFetcher;
import dev.steerup.easyclasscontroller.utils.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ContextBuilder {

    private final List<Class<?>> classes = new ArrayList<>();
    private final Context context = new Context();
    private final String path;

    private ContextBuilder(String path) {
        this.path = path;
    }

    public static ContextBuilder create(String path) {
        return new ContextBuilder(path);
    }

    public ContextBuilder preBuilt(Consumer<Context> preBuiltContextConsumer) {
        preBuiltContextConsumer.accept(this.context);
        return this;
    }

    public ContextBuilder initializeClasses() throws IOException, ClassNotFoundException {
        ClassFetcher
                .create(this.path)
                .createBaseStream()
                .fetch(this.classes::addAll);
        return this;
    }

    public ContextBuilder instantiateClasses() {
        classes.stream()
                .filter(clazz -> clazz.getConstructors()[0].getParameterCount() == 0)
                .forEach(clazz -> {
                    Object instance = instantiateClass(clazz);
                    context.registerComponent(clazz, instance);
                });
        return this;
    }

    public ContextBuilder registerExtraComponents() {
        context.registerComponent(Context.class, this.context);
        return this;
    }

    public ContextBuilder loadProvidedElements() {
        classes.forEach(clazz -> {
            Object component = context.getComponent(clazz);
            Arrays.stream(clazz.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Provide.class))
                    .forEach(field -> {
                        Provide provideAnnotation = field.getAnnotation(Provide.class);

                        String provideAnnotationName = provideAnnotation.value();
                        String name = provideAnnotationName.equals("") ? field.getName() : provideAnnotationName;
                        Object fieldValue = ReflectionUtils.getFieldValue(field, component);

                        context.provide(name, fieldValue);
                    });
        });

        return this;
    }

    public ContextBuilder setProvidedComponents() {
        classes.forEach(clazz -> {
            Object component = context.getComponent(clazz);
            Arrays.stream(clazz.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Load.class))
                    .forEach(field -> {
                        Object providedComponent = context.getComponent(field.getType());

                        ReflectionUtils.setFieldValue(field, component, providedComponent);
                    });
        });
        return this;
    }

    public ContextBuilder setProvidedElements() {
        classes.forEach(clazz -> {
            Object component = context.getComponent(clazz);
            Arrays.stream(clazz.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Fill.class))
                    .forEach(field -> {
                        Fill filAnnotation = field.getAnnotation(Fill.class);

                        String fillAnnotationName = filAnnotation.value();
                        String name = fillAnnotationName.equals("") ? field.getName() : fillAnnotationName;
                        Object providedElement = context.getProvidedElement(name);

                        ReflectionUtils.setFieldValue(field, component, providedElement);
                    });
        });
        return this;
    }

    public ContextBuilder performConstructMethods() {
        Arrays.stream(Construct.Priority.values()).forEach(this::performConstructMethod);
        return this;
    }

    public Context build() {
        return this.context;
    }

    private void performConstructMethod(Construct.Priority priority) {
        classes.forEach(clazz -> {
            Object component = context.getComponent(clazz);
            Arrays.stream(clazz.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(Construct.class))
                    .filter(method -> method.getAnnotation(Construct.class).value().equals(priority))
                    .forEach(method -> ReflectionUtils.invokeMethod(method, component));
        });
    }

    private Object instantiateClass(Class<?> clazz) {
        try {
            return clazz.getConstructors()[0].newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new InternalError("Class " + clazz.getSimpleName() + " could not be instantiated.");
        }
    }
}