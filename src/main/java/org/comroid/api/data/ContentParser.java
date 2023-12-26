package org.comroid.api.data;

import org.comroid.api.data.seri.Serializer;
import org.comroid.api.func.ext.Context;
import org.comroid.api.func.ext.Wrap;
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

    default Wrap<String> wrapContent(boolean createIfAbsent) {
        return Wrap.of(getContent(createIfAbsent));
    }

    default Wrap<String> wrapContent() {
        return wrapContent(false);
    }

    @SuppressWarnings({"unchecked", "removal"}) // todo: Fix removal warning
    default <R> @Nullable R parsefromContext(Context context) {
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
