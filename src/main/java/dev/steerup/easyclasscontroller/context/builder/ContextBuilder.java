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
import dev.steerup.easyclasscontroller.annotations.type.Component;
import dev.steerup.easyclasscontroller.context.Context;
import dev.steerup.easyclasscontroller.context.classes.ClassFetcher;
import dev.steerup.easyclasscontroller.utils.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ContextBuilder {

    private final List<Class<?>> classes = new ArrayList<>();
    private final Class<?> baseClass;
    private final Context context;
    private final String path;

    private ContextBuilder(Class<?> baseClass, String path, Optional<Context> optionalContext) {
        this.baseClass = baseClass;
        this.path = path;
        this.context = optionalContext.orElseGet(Context::new);
    }

    public static ContextBuilder create(Class<?> baseClass, String path, Optional<Context> optionalContext) {
        return new ContextBuilder(baseClass, path, optionalContext);
    }

    public ContextBuilder preBuilt(Consumer<Context> preBuiltContextConsumer) {
        preBuiltContextConsumer.accept(this.context);
        return this;
    }

    public ContextBuilder initializeClasses() throws IOException, ClassNotFoundException {
        ClassFetcher.fetch(this.baseClass, this.path, fetchedClasses -> fetchedClasses
                .stream()
                .filter(clazz -> clazz.isAnnotationPresent(Component.class))
                .forEach(this.classes::add)
        );
        return this;
    }

    public ContextBuilder instantiateClasses() {
        final CopyOnWriteArrayList<Class<?>> classes = new CopyOnWriteArrayList<>(this.classes);

        int maxIterations = this.classes.size() / 5 + 2;
        int iterations = 0;

        while (!classes.isEmpty()) {
            if (iterations >= maxIterations) {
                throw new IllegalArgumentException("Classes could not be instantiated: " + classes.stream().map(clazz -> clazz.getSimpleName() + "(Parameters: " + Arrays.stream(clazz.getConstructors()[0].getParameters()).map(parameter -> parameter.getType().getSimpleName() + " " + parameter.getName()).collect(Collectors.joining(", ")) + ")").collect(Collectors.joining(", ")));
            }
            iterations++;
            for (Class<?> clazz : classes) {
                classes.remove(clazz);
                final Constructor<?> constructor = clazz.getConstructors()[0];
                final int parameterCount = constructor.getParameterCount();

                if (parameterCount == 0) {
                    Object instance = instantiateClass(clazz);
                    context.registerComponent(clazz, instance);
                } else {
                    List<Object> args = new ArrayList<>();
                    for (Parameter parameter : constructor.getParameters()) {
                        final Class<?> parameterType = parameter.getType();
                        final Object parameterInstance = context.getComponent(parameterType);

                        if (parameterInstance != null) {
                            args.add(parameterInstance);
                        } else {
                            final String parameterName = parameter.getName();
                            final Object providedElement = context.getProvidedElement(parameterName);
                            if (providedElement != null) {
                                args.add(providedElement);
                            } else {
                                final Object providedElementByType = context.getProvidedElementByType(parameterType);
                                if (providedElementByType != null) {
                                    args.add(providedElementByType);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    if (args.size() != parameterCount) {
                        classes.add(clazz);
                        continue;
                    }
                    final Object instance = instantiateClass(clazz, args.toArray());
                    context.registerComponent(clazz, instance);
                }
            }
        }

        this.classes.stream()
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
                        Class<?> type = field.getType();
                        Object providedComponent = context.getComponent(type);

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

    private Object instantiateClass(Class<?> clazz, Object... args) {
        try {
            final Constructor<?> constructor = clazz.getConstructors()[0];
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new InternalError("Class " + clazz.getSimpleName() + " could not be instantiated.");
        }
    }
}