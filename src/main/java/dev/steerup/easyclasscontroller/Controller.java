/*
 * Copyright (c) 2021.
 *
 * @author Tobias Cremer
 * @project EasyClassController
 */

package dev.steerup.easyclasscontroller;

import dev.steerup.easyclasscontroller.context.Context;
import dev.steerup.easyclasscontroller.context.builder.ContextBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class Controller {

    private static Context context;

    public static Context attach(String path) throws IOException, ClassNotFoundException {
        return attach(path, preBuiltContext -> {

        });
    }

    public static Context attach(String path, Consumer<Context> preBuiltContextConsumer) throws IOException, ClassNotFoundException {
        if (context == null) {
            throw new InternalError("Context doesn't exist.");
        }

        createContext(path, preBuiltContextConsumer, Optional.of(context));

        return context;
    }

    public static Context initialize(String path) throws IOException, ClassNotFoundException {
        return initialize(path, context -> {
        });
    }

    public static Context initialize(String path, Consumer<Context> preBuiltContextConsumer) throws IOException, ClassNotFoundException {
        return context = createContext(path, preBuiltContextConsumer, Optional.empty());
    }

    private static Context createContext(String path, Consumer<Context> preBuiltContextConsumer, Optional<Context> optionalContext) throws IOException, ClassNotFoundException {
        return ContextBuilder
                .create(path, optionalContext)
                .initializeClasses()
                .instantiateClasses()
                .registerExtraComponents()
                .loadProvidedElements()
                .preBuilt(preBuiltContextConsumer)
                .setProvidedComponents()
                .setProvidedElements()
                .performConstructMethods()
                .build();
    }

    public static Context getContext() {
        return context;
    }
}