package org.comroid.api;

public interface PropertiesHolder {
    <T> Rewrapper<T> getProperty(String name);

    boolean setProperty(String name, Object value);
}
