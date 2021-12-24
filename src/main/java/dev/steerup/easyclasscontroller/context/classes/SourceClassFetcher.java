package dev.steerup.easyclasscontroller.context.classes;

import dev.steerup.easyclasscontroller.annotations.type.Component;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SourceClassFetcher {

    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final Consumer<List<Class<?>>> classesFetchedConsumer;
    private final List<Class<?>> classes = new ArrayList<>();
    private final String path;

    private URL baseResource;

    private SourceClassFetcher(String path, Consumer<List<Class<?>>> classesFetchedConsumer) {
        this.path = path;
        this.classesFetchedConsumer = classesFetchedConsumer;
    }

    public static void fetch(String path, Consumer<List<Class<?>>> classesFetchedConsumer) throws IOException, ClassNotFoundException {
        new SourceClassFetcher(path, classesFetchedConsumer).createBaseStream().start();
    }

    public SourceClassFetcher createBaseStream() {
        baseResource = this.classLoader.getResource(this.path.replaceAll("\\.", "/"));
        assert baseResource != null;
        return this;
    }

    public void start() throws IOException, ClassNotFoundException {
        InputStream inputStream = baseResource.openStream();
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        this.loadClassesOfInputStream(dataInputStream, this.path);
        dataInputStream.close();
        classesFetchedConsumer.accept(this.classes);
    }

    public void loadClassesOfInputStream(DataInputStream dataInputStream, String packet) throws IOException, ClassNotFoundException {
        String read = new String(dataInputStream.readAllBytes());
        String[] split = read.split("\n");
        for (String name : split) {
            if (name.endsWith(".class")) {
                String classIdentifier = (packet.equals("") ? "" : packet + ".") + name.substring(0, name.lastIndexOf('.'));
                this.loadClass(classIdentifier);
            } else {
                String newPacket = packet.equals("") ? name : packet + "." + name;
                this.loadPacket(newPacket);
            }
        }
    }

    private void loadPacket(String packet) throws IOException, ClassNotFoundException {
        URL nextResource = classLoader.getResource(packet.replaceAll("\\.", "/"));
        if (nextResource == null) return;

        DataInputStream nextDataInputStream = new DataInputStream((InputStream) nextResource.getContent());
        this.loadClassesOfInputStream(nextDataInputStream, packet);
        nextDataInputStream.close();
    }

    private void loadClass(String classIdentifier) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(classIdentifier);
        if (!clazz.isAnnotationPresent(Component.class)) return;
        this.classes.add(clazz);
    }
}