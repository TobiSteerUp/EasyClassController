package dev.steerup.easyclasscontroller.custom;

import java.util.List;

/**
 * @author Txb1 at 09.01.2022
 * @project EasyClassController
 */

public interface ClassLoader {
    List<Class<?>> loadClasses(String path);
}