package org.comroid.interaction.component.response;

import org.comroid.interaction.model.Response;
import org.jspecify.annotations.Nullable;

public interface ResponseConverter<T> {
    @Nullable T convertResponse(Object object);

    @Nullable T convertResponse(Response response);
}
