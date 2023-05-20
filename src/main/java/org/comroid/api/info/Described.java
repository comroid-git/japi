package org.comroid.api.info;

public interface Described {
    default String getDescription() {
        return "No description";
    }
}
