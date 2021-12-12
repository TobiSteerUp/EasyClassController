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
import java.util.function.Consumer;

public class Controller {

    private static Context context;

    public static Context initialize(String path) throws IOException, ClassNotFoundException {
        return initialize(path, context -> {
        });
    }

    public static Context initialize(String path, Consumer<Context> preBuiltContextConsumer) throws IOException, ClassNotFoundException {
        Context build = ContextBuilder
                .create(path)
                .preBuilt(preBuiltContextConsumer)
                .initializeClasses()
                .instantiateClasses()
                .registerExtraComponents()
                .loadProvidedElements()
                .setProvidedComponents()
                .setProvidedElements()
                .performConstructMethods()
                .build();
        context = build;
        return build;
    }

    public static Context getContext() {
        return context;
    }
}