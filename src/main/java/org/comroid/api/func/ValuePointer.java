package org.comroid.api.func;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import org.comroid.annotations.Ignore;
import org.comroid.api.data.seri.type.ValueType;

@Ignore
public interface ValuePointer<T> {
    @Transient
    @JsonIgnore
    ValueType<? extends T> getHeldType();
}
