package org.comroid.api.func;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.comroid.annotations.Ignore;
import org.comroid.api.data.seri.type.ValueType;

import javax.persistence.Transient;

@Ignore
public interface ValuePointer<T> {
    @Transient
    @JsonIgnore
    ValueType<? extends T> getHeldType();
}
