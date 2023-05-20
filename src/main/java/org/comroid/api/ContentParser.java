package org.comroid.api;

import org.comroid.util.ReaderUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.io.StringReader;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An object whose content can be parsed using a {@link Serializer}
 */
public interface ContentParser extends Readable {
    default String getContent() {
        return getContent(false);
    }

    String getContent(boolean createIfAbsent);

    @Override
    default Reader toReader() {
        String content = getContent(false);
        if (content == null)
            return ReaderUtil.empty();
        return new StringReader(content);
    }

    default Rewrapper<String> wrapContent(boolean createIfAbsent) {
        return Rewrapper.of(getContent(createIfAbsent));
    }

    default Rewrapper<String> wrapContent() {
        return wrapContent(false);
    }

    default <R> @Nullable R parsefromContext(Context context) {
        //noinspection unchecked
        return context.getFromContext(Serializer.class)
                .ifPresentMap(serializer -> parse((Serializer<R>) serializer));
    }

    default <R> @NotNull R parse(final Serializer<R> serializer) {
        return parse(serializer, serializer::createObjectNode);
    }

    default <R> R parse(final Function<String, R> parser, final Supplier<R> elseGet) {
        try {
            return wrapContent().ifPresentMapOrElseGet(parser, elseGet);
        } catch (IllegalArgumentException parseException) {
            return elseGet.get();
        }
    }
}
