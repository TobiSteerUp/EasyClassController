/*
 * Copyright (c) 2021.
 *
 * @author Tobias Cremer
 * @project EasyClassController
 *
 * @Credits thanks to Lukas Ringel
 */

package dev.steerup.easyclasscontroller.context.classes;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarClassFetcher {

    private static final int BUFFER_SIZE = 4096;

    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final Map<String, byte[]> classBytesMap = new HashMap<>();
    private final Consumer<List<Class<?>>> classesFetchedConsumer;
    private final List<Class<?>> classes = new ArrayList<>();
    private final Class<?> baseClass;
    private final String path;

    private JarClassFetcher(Class<?> baseClass, String path, Consumer<List<Class<?>>> classesFetchedConsumer) {
        this.classesFetchedConsumer = classesFetchedConsumer;
        this.baseClass = baseClass;
        this.path = path;
    }

    public static void fetch(Class<?> baseClass, String path, Consumer<List<Class<?>>> classesFetchedConsumer) throws IOException {
        new JarClassFetcher(baseClass, path, classesFetchedConsumer).start();
    }

    public void start() throws IOException {
        URL location = this.baseClass.getProtectionDomain().getCodeSource().getLocation();
        JarFile jarFile = new JarFile(location.getPath());
        loadClasses(jarFile);
        classesFetchedConsumer.accept(this.classes);
    }

    private void loadClasses(JarFile jarFile) {
        Enumeration<JarEntry> entries = jarFile.entries();
        String s = this.path.replaceAll("[.]", "/");
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            if (!name.endsWith(".class")) continue;
            if (!name.startsWith(s)) continue;

            String className = name.replace(".class", "").replaceAll("/", ".");
            try {
                Class<?> clazz = classLoader.loadClass(className);
                this.classes.add(clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}