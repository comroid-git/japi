package org.comroid.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.comroid.annotations.Ignore;

import javax.persistence.Transient;

@Ignore
public interface ValuePointer<T> {
    @Transient
    @JsonIgnore
    ValueType<? extends T> getHeldType();
}
