/*
 * Copyright (c) 2021.
 *
 * @author Tobias Cremer
 * @project EasyClassController
 */

package dev.steerup.easyclasscontroller.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Context {

    private final Map<Class<?>, Object> componentMap = new HashMap<>();
    private final Map<String, Object> providedObjectsMap = new HashMap<>();

    public void provide(String name, Object object) {
        this.providedObjectsMap.put(name, object);
    }

    public void registerComponent(Class<?> clazz, Object instance) {
        this.componentMap.put(clazz, instance);
    }

    public Object getProvidedElement(String name) {
        return this.providedObjectsMap.get(name);
    }

    public <T> T getComponent(Class<T> clazz) {
        return clazz.cast(this.componentMap.get(clazz));
    }

    public <T> List<T> getComponents(Class<T> type) {
        return ((List<T>) this.componentMap
                .values()
                .stream()
                .filter(component -> component.getClass().isAssignableFrom(type))
                .collect(Collectors.toList()));
    }

    public Map<Class<?>, Object> getComponents() {
        return componentMap;
    }
}