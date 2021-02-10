package org.comroid.api;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ContentParser {
    String getContent(boolean createIfAbsent);

    default String getContent() {
        return getContent(false);
    }

    default Rewrapper<String> wrapContent(boolean createIfAbsent) {
        return Rewrapper.of(getContent(createIfAbsent));
    }

    default Rewrapper<String> wrapContent() {
        return wrapContent(false);
    }

    default <R> @NotNull R parse(final Serializer<R> serializer) {
        return parse(serializer, serializer::createObjectNode);
    }

    default <R> R parse(final Function<String, R> parser, final Supplier<R> elseGet) {
        return wrapContent().ifPresentMapOrElseGet(parser, elseGet);
    }
}
