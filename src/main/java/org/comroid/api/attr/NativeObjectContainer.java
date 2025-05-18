package org.comroid.api.attr;

import lombok.Data;

@Data
public class NativeObjectContainer<T> {
    protected T nativeObject;
}
