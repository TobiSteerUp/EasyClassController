package dev.steerup.easyclasscontroller.context.classes;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ClassFetcher {

    public static void fetch(Class<?> baseClass, String path, Consumer<List<Class<?>>> classesFetchedConsumer) throws IOException, ClassNotFoundException {
        try {
            JarClassFetcher.fetch(baseClass, path, classesFetchedConsumer);
        } catch (IOException exception) {
            SourceClassFetcher.fetch(path, classesFetchedConsumer);
        }
    }
}